package com.kutoru.mikunotes.viewmodel

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.kutoru.mikunotes.R
import com.kutoru.mikunotes.utils.DOWNLOAD_NOTIFICATION_CHANNEL_ID
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.prepareGet
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentLength
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.isEmpty
import io.ktor.utils.io.core.readBytes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar

// https://ktor.io/docs/client-serialization.html
// https://ktor.io/docs/client-cookies.html

class ApiService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private val client = HttpClient()
    private val apiUrl = "http://192.168.1.12:3030"
    private val cookieValue = "at=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjE3MjI0Mzk4NzcsImlhdCI6MTcyMjQzMjY3Nywic3ViIjoiMSJ9.fyCn-_vEwFDFkb1V1y5e8UYU52HJ6OTmsp1KDsnoCgY"
    private var downloadIdx = 0

    private lateinit var notificationManager: NotificationManagerCompat

    override fun onCreate() {
        super.onCreate()
        notificationManager = NotificationManagerCompat.from(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val fileHash = intent!!.getStringExtra("FILE_HASH")!!

        scope.launch {
            try {
//                getShelf()
                getFile(fileHash)
            } catch (e: Throwable) {
                println("Got an err: $e")
            }
        }

        return START_STICKY
    }

    private suspend fun getShelf() {
        val url = "$apiUrl/shelf"
        val res = client.get(url) {
            this.headers.append("Cookie", cookieValue)
        }

        if (!handleHttpStatus(res.status)) {
            return
        }

        val body: String = res.body()
        println(body)
    }

    @SuppressLint("MissingPermission")
    private suspend fun getFile(fileHash: String) {
        val currentDownloadIdx = ++downloadIdx

        val url = "$apiUrl/files/dl/$fileHash"
        val req = client.prepareGet(url){
            this.headers.append("Cookie", cookieValue)
        }

        val res = req.execute()
        if (!handleHttpStatus(res.status)) {
            return
        }

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

        val notification = getDownloadNotificationBuilder(fileName)
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
            val notification = getDownloadFinishedNotification(file)
            notificationManager.notify(currentDownloadIdx, notification)
        }

        println("A file saved to ${file.path}")
    }

    private fun getDownloadNotificationBuilder(fileName: String): NotificationCompat.Builder {
        return NotificationCompat
            .Builder(applicationContext, DOWNLOAD_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_download)
            .setContentTitle("Download")
            .setContentText(fileName)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, 0, false)
            .setAutoCancel(true)
    }

    private fun getDownloadFinishedNotification(file: File): Notification {
        val uri = FileProvider.getUriForFile(applicationContext, applicationContext.packageName + ".provider", file)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat
            .Builder(applicationContext, DOWNLOAD_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_download)
            .setContentTitle("Download")
            .setContentText("${file.name} has been downloaded")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(false)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
    }

    private fun notificationPermissionGranted(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true
        }

        return ActivityCompat.checkSelfPermission(
            applicationContext, Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun handleHttpStatus(status: HttpStatusCode): Boolean {
        println("Got a response with $status")
        return when (status) {
            HttpStatusCode.OK, HttpStatusCode.Created -> true
            else -> false
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        println("service onbind")
        return null
    }
}
