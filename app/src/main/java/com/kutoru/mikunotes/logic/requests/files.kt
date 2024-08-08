package com.kutoru.mikunotes.logic.requests

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import com.kutoru.mikunotes.logic.NotificationHelper
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

suspend fun RequestManager.postFileToNote(fileUri: Uri, noteId: Int): File {
    return postFile(fileUri, noteId, "note_id")
}

suspend fun RequestManager.postFileToShelf(filePath: Uri, shelfId: Int): File {
    return postFile(filePath, shelfId, "shelf_id")
}

@SuppressLint("MissingPermission")
suspend fun RequestManager.postFile(fileUri: Uri, attachId: Int, attachKey: String): File {
    val currNotificationIndex = ++notificationIndex

    val fileStream = context.contentResolver.openInputStream(fileUri)!!
    val fileName = getFileInfo(context.contentResolver, fileUri, OpenableColumns.DISPLAY_NAME)
    val fileSize = getFileInfo(context.contentResolver, fileUri, OpenableColumns.SIZE).toFloat()

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
    var notification = NotificationHelper.getUploadInProgress(context, fileName)
    if (NotificationHelper.canSend(context)) {
        notificationManager.notify(currNotificationIndex, notification.build())
    }

    val req = httpClient.prepareFormWithBinaryData("$apiUrl/files", form) {
        headers.append("Cookie", accessCookie)

        onUpload { bytesSentTotal, _ ->
            val progress = ((bytesSentTotal / fileSize) * 100).toInt()
            notification.setProgress(100, progress, false)

            val current = Calendar.getInstance().timeInMillis
            if (NotificationHelper.canSend(context) && lastUpdated + 1000 <= current) {
                lastUpdated = current
                notificationManager.notify(currNotificationIndex, notification.build())
            }
        }
    }

    val fileInfo = try {
        executeRequestUntilBody<File>(req)
    } finally {
        fileStream.close()
    }

    notification = NotificationHelper.getUploadFinished(context, fileName)
    if (NotificationHelper.canSend(context)) {
        notificationManager.notify(currNotificationIndex, notification.build())
    }

    return fileInfo
}

@SuppressLint("MissingPermission")
suspend fun RequestManager.getFile(fileHash: String) {
    val currNotifIndex = ++notificationIndex
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

    val notification = NotificationHelper.getDownloadInProgress(context, fileName)
    if (NotificationHelper.canSend(context)) {
        notificationManager.notify(currNotifIndex, notification.build())
    }

    val channel: ByteReadChannel = res.body()
    var lastUpdated = Calendar.getInstance().timeInMillis

    while (!channel.isClosedForRead) {
        val packet = channel.readRemaining(DEFAULT_BUFFER_SIZE.toLong())

        while (!packet.isEmpty) {
            val bytes = packet.readBytes()
            file.appendBytes(bytes)

            val progress = ((file.length() / res.contentLength()!!.toFloat()) * 100).toInt()
            notification.setProgress(100, progress, false)

            val current = Calendar.getInstance().timeInMillis
            if (NotificationHelper.canSend(context) && lastUpdated + 1000 <= current) {
                lastUpdated = current
                notificationManager.notify(currNotifIndex, notification.build())
            }
        }
    }

    if (NotificationHelper.canSend(context)) {
        val notification = NotificationHelper.getDownloadFinished(context, file)
        notificationManager.notify(currNotifIndex, notification.build())
    }

    println("A file saved to ${file.path}")
}

suspend fun RequestManager.deleteFile(fileId: Int) {
    executeRequestUntilResponse("$apiUrl/files/$fileId", HttpMethod.Delete)
}
