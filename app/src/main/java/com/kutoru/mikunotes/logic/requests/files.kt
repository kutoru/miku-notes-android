package com.kutoru.mikunotes.logic.requests

import android.content.ContentResolver
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import com.kutoru.mikunotes.models.File
import io.ktor.client.call.body
import io.ktor.client.plugins.onUpload
import io.ktor.client.request.forms.InputProvider
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.prepareFormWithBinaryData
import io.ktor.http.HeadersBuilder
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.contentLength
import io.ktor.utils.io.ByteReadChannel
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

suspend fun RequestManager.postFileToNote(contentResolver: ContentResolver, fileUri: Uri, noteId: Int): File {
    return postFile(contentResolver, fileUri, noteId, "note_id")
}

suspend fun RequestManager.postFileToShelf(contentResolver: ContentResolver, filePath: Uri, shelfId: Int): File {
    return postFile(contentResolver, filePath, shelfId, "shelf_id")
}

private suspend fun RequestManager.postFile(contentResolver: ContentResolver, fileUri: Uri, attachId: Int, attachKey: String): File {
    val fileStream = contentResolver.openInputStream(fileUri)!!
    val fileName = getFileInfo(contentResolver, fileUri, OpenableColumns.DISPLAY_NAME)
    val fileSize = getFileInfo(contentResolver, fileUri, OpenableColumns.SIZE).toFloat()

    val form = formData {
        append(attachKey, "$attachId")

        val headersBuilder = HeadersBuilder()
        headersBuilder.append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")

        append(
            "file",
            InputProvider { fileStream.asInput() },
            headersBuilder.build(),
        )
    }

    var lastUpdated = Calendar.getInstance().timeInMillis
    val currentNotificationIndex = notificationHelper.showUploadInProgress(null, fileName, 0)

    val req = httpClient.prepareFormWithBinaryData("$apiUrl/files", form) {
        headers.append("Cookie", accessCookie)

        onUpload { bytesSentTotal, _ ->
            val progress = ((bytesSentTotal / fileSize) * 100).toInt()
            val currentTime = Calendar.getInstance().timeInMillis

            if (lastUpdated + 1000 <= currentTime) {
                lastUpdated = currentTime
                notificationHelper.showUploadInProgress(currentNotificationIndex, fileName, progress)
            }
        }
    }

    val fileInfo = try {
        executeRequestUntilBody<File>(req)
    } finally {
        fileStream.close()
    }

    notificationHelper.showUploadFinished(currentNotificationIndex, fileName)

    return fileInfo
}

suspend fun RequestManager.getFile(fileHash: String) {
    val res = executeRequestUntilResponse("$apiUrl/files/dl/$fileHash", HttpMethod.Get)

    val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path
    var fileName = res.headers["content-disposition"]?.split('=')?.get(1)?.trim('"') ?: "file.bin"
    var file = java.io.File("$downloadDir/$fileName")

    var fileIdx = 0
    val (filePrefix, fileExt) = fileName.split('.')

    while (!file.createNewFile()) {
        fileIdx++
        fileName = "$filePrefix ($fileIdx).$fileExt"
        file = java.io.File("$downloadDir/$fileName")
    }

    val currentNotificationIndex = notificationHelper.showDownloadInProgress(null, fileName, 0)

    val channel: ByteReadChannel = res.body()
    var lastUpdated = Calendar.getInstance().timeInMillis

    while (!channel.isClosedForRead) {
        val packet = channel.readRemaining(DEFAULT_BUFFER_SIZE.toLong())

        while (!packet.isEmpty) {
            val bytes = packet.readBytes()
            file.appendBytes(bytes)

            val progress = if (res.contentLength() != null) {
                ((file.length() / res.contentLength()!!.toFloat()) * 100).toInt()
            } else {
                100
            }

            val currentTime = Calendar.getInstance().timeInMillis
            if (lastUpdated + 1000 <= currentTime) {
                lastUpdated = currentTime
                notificationHelper.showDownloadInProgress(currentNotificationIndex, fileName, progress)
            }
        }
    }

    notificationHelper.showDownloadFinished(currentNotificationIndex, file)
    println("A file saved to ${file.path}")
}

suspend fun RequestManager.deleteFile(fileId: Int) {
    executeRequestUntilResponse("$apiUrl/files/$fileId", HttpMethod.Delete)
}
