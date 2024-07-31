package com.kutoru.mikunotes.logic

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import com.google.protobuf.compiler.PluginProtos
import com.kutoru.mikunotes.R
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.prepareGet
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentLength
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.isEmpty
import io.ktor.utils.io.core.readBytes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import shelves.ShelfKt
import shelves.ShelvesOuterClass.Shelf
import java.io.File
import java.util.Calendar

// https://ktor.io/docs/client-serialization.html
// https://ktor.io/docs/client-cookies.html

class ApiService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private var downloadIdx = 0

    private val apiUrl = "http://192.168.1.12:3030"
    private val cookieValue = "at=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjE3MjI0NTQwOTQsImlhdCI6MTcyMjQ0Njg5NCwic3ViIjoiMSJ9.TwQRyHu9w-8pJ-CwLtu_NZ20EUK2Oze2z248GEpHS5Y"
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
    }

    private lateinit var notificationManager: NotificationManagerCompat

    override fun onCreate() {
        super.onCreate()
        notificationManager = NotificationManagerCompat.from(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val fileHash = intent!!.getStringExtra("FILE_HASH")!!

        scope.launch {
            try {
                getShelf()
//                getFile(fileHash)
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

        val body: Shelf = res.body()
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

        val notification = NotificationHelper.getDownloadInProgress(applicationContext, fileName)
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
            val notification = NotificationHelper.getDownloadFinished(applicationContext, file)
            notificationManager.notify(currentDownloadIdx, notification.build())
        }

        println("A file saved to ${file.path}")
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
