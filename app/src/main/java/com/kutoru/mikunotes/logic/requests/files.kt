package com.kutoru.mikunotes.logic.requests

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import com.kutoru.mikunotes.logic.NotificationHelper
import com.kutoru.mikunotes.models.File
import com.kutoru.mikunotes.models.ResultBody
import io.ktor.client.call.body
import io.ktor.client.request.forms.InputProvider
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.prepareFormWithBinaryData
import io.ktor.client.request.prepareDelete
import io.ktor.client.request.prepareGet
import io.ktor.http.HeadersBuilder
import io.ktor.http.HttpHeaders
import io.ktor.http.contentLength
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.isEmpty
import io.ktor.utils.io.core.readBytes
import io.ktor.utils.io.streams.asInput
import java.util.Calendar

private fun getFileName(contentResolver: ContentResolver, fileUri: Uri): String {
    val returnCursor = contentResolver.query(fileUri, null, null, null, null)!!
    val nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
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
    val currNotifIndex = ++notificationIndex

    val fileStream = context.contentResolver.openInputStream(fileUri)!!
    val fileName = getFileName(context.contentResolver, fileUri)

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

    val url = "$apiUrl/files"
    val req = httpClient.prepareFormWithBinaryData(url, form) {
        headers.append("Cookie", accessCookie)
    }

    var notification = NotificationHelper.getUploadInProgress(context, fileName)
    if (notificationPermissionGranted()) {
        notificationManager.notify(currNotifIndex, notification.build())
    }

    val res = executeRequest(req)
    fileStream.close()
    handleHttpStatus(res.status)

    notification = NotificationHelper.getUploadFinished(context, fileName)
    if (notificationPermissionGranted()) {
        notificationManager.notify(currNotifIndex, notification.build())
    }

    val body: ResultBody<File> = res.body()
    return handleBody(body)
}

@SuppressLint("MissingPermission")
suspend fun RequestManager.getFile(fileHash: String) {
    val currNotifIndex = ++notificationIndex

    val url = "$apiUrl/files/dl/$fileHash"
    val req = httpClient.prepareGet(url) {
        headers.append("Cookie", accessCookie)
    }

    val res = executeRequest(req)
    handleHttpStatus(res.status)

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
    if (notificationPermissionGranted()) {
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
            if (notificationPermissionGranted() && lastUpdated + 1000 <= current) {
                lastUpdated = current
                notificationManager.notify(currNotifIndex, notification.build())
            }
        }
    }

    if (notificationPermissionGranted()) {
        val notification = NotificationHelper.getDownloadFinished(context, file)
        notificationManager.notify(currNotifIndex, notification.build())
    }

    println("A file saved to ${file.path}")
}

suspend fun RequestManager.deleteFile(fileId: Int) {
    val url = "$apiUrl/files/$fileId"
    val req = httpClient.prepareDelete(url) {
        headers.append("Cookie", accessCookie)
    }

    val res = executeRequest(req)
    handleHttpStatus(res.status)
}
