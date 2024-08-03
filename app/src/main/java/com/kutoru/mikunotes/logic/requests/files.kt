package com.kutoru.mikunotes.logic.requests

import android.annotation.SuppressLint
import android.os.Environment
import com.kutoru.mikunotes.logic.NotificationHelper
import io.ktor.client.call.body
import io.ktor.client.request.prepareDelete
import io.ktor.client.request.prepareGet
import io.ktor.http.contentLength
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.isEmpty
import io.ktor.utils.io.core.readBytes
import java.io.File
import java.util.Calendar

@SuppressLint("MissingPermission")
suspend fun RequestManager.getFile(fileHash: String) {
    val currentDownloadIdx = ++downloadIdx

    val url = "$apiUrl/files/dl/$fileHash"
    val req = httpClient.prepareGet(url) {
        headers.append("Cookie", accessCookie)
    }

    val res = executeRequest(req)
    handleHttpStatus(res.status)

    val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path
    var fileName = res.headers["content-disposition"]?.split('=')?.get(1)?.trim('"') ?: "file.bin"
    var file = File("$downloadDir/$fileName")

    var fileIdx = 0
    val (filePrefix, fileExt) = fileName.split('.')

    while (!file.createNewFile()) {
        fileIdx++
        fileName = "$filePrefix ($fileIdx).$fileExt"
        file = File("$downloadDir/$fileName")
    }

    val notification = NotificationHelper.getDownloadInProgress(context, fileName)
    if (notificationPermissionGranted()) {
        notificationManager.notify(currentDownloadIdx, notification.build())
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
                notificationManager.notify(currentDownloadIdx, notification.build())
            }
        }
    }

    if (notificationPermissionGranted()) {
        val notification = NotificationHelper.getDownloadFinished(context, file)
        notificationManager.notify(currentDownloadIdx, notification.build())
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
