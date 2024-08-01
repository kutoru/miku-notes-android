package com.kutoru.mikunotes.logic

import android.Manifest
import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import android.os.Environment
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import com.kutoru.mikunotes.models.LoginBody
import com.kutoru.mikunotes.models.ResultBody
import com.kutoru.mikunotes.models.Shelf
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.ConstantCookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.prepareGet
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Cookie
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentLength
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.isEmpty
import io.ktor.utils.io.core.readBytes
import java.io.File
import java.net.HttpCookie
import java.util.Calendar

// https://ktor.io/docs/client-requests.html#upload_file

class ApiService : Service() {

    private val binder = ServiceBinder()
    private var downloadIdx = 0

    private lateinit var notificationManager: NotificationManagerCompat
    private lateinit var persistentStorage: SharedPreferences
    private lateinit var httpClient: HttpClient

    override fun onCreate() {
        super.onCreate()
        notificationManager = NotificationManagerCompat.from(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        persistentStorage = getSharedPreferences(PREFERENCE_FILE_KEY, Context.MODE_PRIVATE)

        val accessCookieValue = persistentStorage.getString(ACCESS_TOKEN_COOKIE_KEY, null)
        val refreshCookieValue = persistentStorage.getString(REFRESH_TOKEN_COOKIE_KEY, null)

        val parsedAccessCookie = HttpCookie.parse(accessCookieValue)[0]
        val accessCookie = Cookie(
            name = parsedAccessCookie.name,
            value = parsedAccessCookie.value,
            maxAge = parsedAccessCookie.maxAge.toInt(),
            domain = API_DOMAIN,
        )

        val parsedRefreshCookie = HttpCookie.parse(refreshCookieValue)[0]
        val refreshCookie = Cookie(
            name = parsedRefreshCookie.name,
            value = parsedRefreshCookie.value,
            maxAge = parsedRefreshCookie.maxAge.toInt(),
            domain = API_DOMAIN,
        )

        httpClient = HttpClient(CIO) {
            install(ContentNegotiation) {
                json()
            }

            install(HttpCookies) {
                storage = ConstantCookiesStorage(accessCookie, refreshCookie)
            }
        }

        return START_STICKY
    }

    suspend fun getShelf(): Shelf? {
        val url = "$API_URL/shelf"
        val res = httpClient.get(url)

        if (!handleHttpStatus(res.status)) {
            return null
        }

        val body: ResultBody<Shelf> = res.body()
//        println(body)
        return body.data
    }

    suspend fun login(loginBody: LoginBody) {
        val url = "$API_URL/login"

        val res = httpClient.post(url) {
            contentType(ContentType.Application.Json)
            setBody(loginBody)
        }

        if (!handleHttpStatus(res.status)) {
            return
        }

        val cookies = res.headers.getAll("set-cookie")
        if (cookies == null || cookies.size != 2) {
            return
        }

        if (cookies[0].startsWith("at=")) {
            persistentStorage.edit {
                putString(ACCESS_TOKEN_COOKIE_KEY, cookies[0])
                putString(REFRESH_TOKEN_COOKIE_KEY, cookies[1])
            }
        } else {
            persistentStorage.edit {
                putString(ACCESS_TOKEN_COOKIE_KEY, cookies[1])
                putString(REFRESH_TOKEN_COOKIE_KEY, cookies[0])
            }
        }

        val body: ResultBody<Unit> = res.body()
        println(body)
    }

    @SuppressLint("MissingPermission")
    suspend fun getFile(fileHash: String) {
        val currentDownloadIdx = ++downloadIdx

        val url = "$API_URL/files/dl/$fileHash"
        val req = httpClient.prepareGet(url)

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
        return binder
    }

    inner class ServiceBinder : Binder() {
        fun getService(): ApiService {
            return this@ApiService
        }
    }
}
