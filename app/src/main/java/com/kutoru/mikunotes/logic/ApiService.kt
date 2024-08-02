package com.kutoru.mikunotes.logic

import android.Manifest
import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import android.os.Environment
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import com.kutoru.mikunotes.models.LoginBody
import com.kutoru.mikunotes.models.ResultBody
import com.kutoru.mikunotes.models.Shelf
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.prepareGet
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.HttpStatement
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLParserException
import io.ktor.http.contentLength
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.isEmpty
import io.ktor.utils.io.core.readBytes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import java.io.File
import java.net.ConnectException
import java.nio.channels.UnresolvedAddressException
import java.util.Calendar

// https://ktor.io/docs/client-requests.html#upload_file

class ApiService : Service() {

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    private val binder = ServiceBinder()
    private var downloadIdx = 0

    private var apiDomain: String? = null
    private lateinit var apiUrl: String
    private lateinit var accessCookie: String
    private lateinit var refreshCookie: String

    private lateinit var notificationManager: NotificationManagerCompat
    private lateinit var persistentStorage: PersistentStorage
    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = NotificationManagerCompat.from(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        persistentStorage = PersistentStorage(applicationContext)

        updateUrl()
        updateCookies()

        return START_STICKY
    }

    override fun onDestroy() {
        job.cancel()
        super.onDestroy()
    }

    fun updateUrl() {
        val domain = persistentStorage.domain
        val port = persistentStorage.port ?: 0
        val isSecure = persistentStorage.isSecure

        apiDomain = domain
        val protocol = if (isSecure != null && isSecure) "https" else "http"
        apiUrl = "$protocol://$domain:$port"
    }

    private fun updateCookies() {
        accessCookie = persistentStorage.accessCookie ?: ""
        refreshCookie = persistentStorage.refreshCookie ?: ""
    }

    suspend fun getShelf(): Shelf? {
        val url = "$apiUrl/shelf"
        val req = httpClient.prepareGet(url) {
            headers.append("Cookie", accessCookie)
        }

        val res = handleRequest(req)
        handleHttpStatus(res.status)

        val body: ResultBody<Shelf> = res.body()
        return body.data
    }

    suspend fun login(loginBody: LoginBody) {
        val url = "$apiUrl/login"
        val req = httpClient.preparePost(url) {
            contentType(ContentType.Application.Json)
            setBody(loginBody)
        }

        val res = handleRequest(req)
        handleHttpStatus(res.status)

        val rawCookies = res.headers.getAll("set-cookie")
        if (rawCookies == null || rawCookies.size != 2) {
            throw ServerError()
        }

        val cookies = rawCookies.map { it.split(';')[0] }

        if (cookies[0].startsWith("at=")) {
            persistentStorage.accessCookie = cookies[0]
            persistentStorage.refreshCookie = cookies[1]
        } else {
            persistentStorage.accessCookie = cookies[1]
            persistentStorage.refreshCookie = cookies[0]
        }

        updateCookies()
    }

    @SuppressLint("MissingPermission")
    suspend fun getFile(fileHash: String) {
        val currentDownloadIdx = ++downloadIdx

        val url = "$apiUrl/files/dl/$fileHash"
        val req = httpClient.prepareGet(url) {
            headers.append("Cookie", accessCookie)
        }

        val res = handleRequest(req)
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

    suspend fun access() {
        val url = "$apiUrl/access"
        val req = httpClient.prepareGet(url) {
            headers.append("Cookie", refreshCookie)
        }

        val res = handleRequest(req)
        handleHttpStatus(res.status)

        val accessCookie = res.headers["set-cookie"] ?: throw ServerError()
        persistentStorage.accessCookie = accessCookie

        updateCookies()
    }

    private fun notificationPermissionGranted(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true
        }

        return ActivityCompat.checkSelfPermission(
            applicationContext, Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private suspend fun handleRequest(request: HttpStatement): HttpResponse {
        try {
            return request.execute()
        } catch (e: Exception) {
            throw when (e) {
                is ConnectException,
                is UnresolvedAddressException,
                is URLParserException -> InvalidUrl()
                else -> e
            }
        }
    }

    private fun handleHttpStatus(status: HttpStatusCode) {
        println("Got a response with $status")
        when (status.value) {
            200, 201 -> {}
            401 -> throw Unauthorized()
            in 400..<500 -> throw BadRequest()
            500 -> throw ServerError()
            else -> throw UnknownError()
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    inner class ServiceBinder : Binder() {
        fun getService(): ApiService {
            return this@ApiService
        }
    }
}
