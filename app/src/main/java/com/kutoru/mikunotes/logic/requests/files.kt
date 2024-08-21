package com.kutoru.mikunotes.logic.requests

import android.app.job.JobParameters
import android.content.ContentResolver
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import com.kutoru.mikunotes.logic.RequestCancel
import com.kutoru.mikunotes.models.File
import io.ktor.client.call.body
import io.ktor.client.plugins.onUpload
import io.ktor.client.plugins.timeout
import io.ktor.client.request.forms.InputProvider
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.prepareFormWithBinaryData
import io.ktor.http.HeadersBuilder
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.contentLength
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.cancel
import io.ktor.utils.io.core.isEmpty
import io.ktor.utils.io.core.readBytes
import io.ktor.utils.io.streams.asInput
import java.util.Calendar

private fun getFileInfo(contentResolver: ContentResolver, fileUri: Uri, openableColumn: String): String {
    val returnCursor = contentResolver.query(fileUri, null, null, null, null)!!
    val nameIndex = returnCursor.getColumnIndex(openableColumn)
    returnCursor.moveToFirst()
    val name = returnCursor.getString(nameIndex)
    returnCursor.close()
    return name
}

suspend fun RequestManager.postFileToNote(
    contentResolver: ContentResolver,
    fileUri: Uri,
    noteId: Int,
    notificationId: Int,
    params: JobParameters?,
): File {
    return postFile(contentResolver, fileUri, noteId, "note_id", notificationId, params)
}

suspend fun RequestManager.postFileToShelf(
    contentResolver: ContentResolver,
    filePath: Uri,
    shelfId: Int,
    notificationId: Int,
    params: JobParameters?,
): File {
    return postFile(contentResolver, filePath, shelfId, "shelf_id", notificationId, params)
}

private suspend fun RequestManager.postFile(
    contentResolver: ContentResolver,
    fileUri: Uri,
    attachId: Int,
    attachKey: String,
    notificationId: Int,
    params: JobParameters?,
): File {

    // preparing request data
    val fileStream = contentResolver.openInputStream(fileUri)!!
    val fileName = getFileInfo(contentResolver, fileUri, OpenableColumns.DISPLAY_NAME)
    val fileSize = getFileInfo(contentResolver, fileUri, OpenableColumns.SIZE)

    val form = formData {
        append(attachKey, "$attachId")
        append("file_size", fileSize)

        val headersBuilder = HeadersBuilder()
        headersBuilder.append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")

        append(
            "file",
            InputProvider { fileStream.asInput() },
            headersBuilder.build(),
        )
    }

    // uploading the file
    var lastUpdated = Calendar.getInstance().timeInMillis
    notificationHelper.showUploadInProgress(notificationId, fileName, 0, params)

    val req = httpClient.prepareFormWithBinaryData("$apiUrl/files", form) {
        timeout { this.requestTimeoutMillis = 86_400_000 }
        headers.append("Cookie", accessCookie)

        onUpload { bytesSentTotal, _ ->
            // on upload cancel
            if (requestsToStop.contains(notificationId)) {
                fileStream.close()

                requestsToStop.remove(notificationId)

                throw RequestCancel("Upload has been cancelled by the user")
            }

            // notification stuff
            val currentTime = Calendar.getInstance().timeInMillis

            if (lastUpdated + 1000 <= currentTime) {
                lastUpdated = currentTime

                val progress = ((bytesSentTotal / fileSize.toFloat()) * 100).toInt()
                notificationHelper.showUploadInProgress(notificationId, fileName, progress, params)
            }
        }
    }

    val fileInfo = try {
        executeRequestUntilBody<File>(req)
    } finally {
        fileStream.close()
    }

    notificationHelper.showUploadFinished(notificationId, fileName, params)

    return fileInfo
}

suspend fun RequestManager.getFile(
    fileHash: String,
    notificationId: Int,
    params: JobParameters?,
) {
    val res = executeRequestUntilResponse("$apiUrl/files/dl/$fileHash", HttpMethod.Get, requestTimeout = 86_400_000)
    val contentLength = res.contentLength()

    // getting file path
    val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path
    var fileName = res.headers["content-disposition"]?.split('=')?.get(1)?.trim('"') ?: "file.bin"
    var file = java.io.File("$downloadDir/$fileName")

    // updating the file name if it already exists
    var fileIdx = 0
    val (filePrefix, fileExt) = fileName.split('.')

    while (!file.createNewFile()) {
        fileIdx++
        fileName = "$filePrefix ($fileIdx).$fileExt"
        file = java.io.File("$downloadDir/$fileName")
    }

    // the download itself
    notificationHelper.showDownloadInProgress(notificationId, fileName, 0, params)

    val channel: ByteReadChannel = res.body()
    var lastUpdated = Calendar.getInstance().timeInMillis

    while (!channel.isClosedForRead) {
        val packet = channel.readRemaining(DEFAULT_BUFFER_SIZE.toLong())

        while (!packet.isEmpty) {

            // on download cancel
            if (requestsToStop.contains(notificationId)) {
                channel.cancel()
                file.delete()

                requestsToStop.remove(notificationId)

                throw RequestCancel("Download has been cancelled by the user")
            }

            // appending data
            val bytes = packet.readBytes()
            file.appendBytes(bytes)

            // handling notification
            val currentTime = Calendar.getInstance().timeInMillis

            if (lastUpdated + 1000 <= currentTime) {
                lastUpdated = currentTime

                val progress = if (contentLength != null) {
                    ((file.length() / contentLength.toFloat()) * 100).toInt()
                } else {
                    100
                }

                notificationHelper.showDownloadInProgress(notificationId, fileName, progress, params)
            }
        }
    }

    notificationHelper.showDownloadFinished(notificationId, file, params)
}

suspend fun RequestManager.deleteFile(fileId: Int) {
    executeRequestUntilResponse("$apiUrl/files/$fileId", HttpMethod.Delete)
}
