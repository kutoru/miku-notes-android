package com.kutoru.mikunotes.logic.requests

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import com.kutoru.mikunotes.logic.BadRequest
import com.kutoru.mikunotes.logic.InvalidUrl
import com.kutoru.mikunotes.logic.PersistentStorage
import com.kutoru.mikunotes.logic.ServerError
import com.kutoru.mikunotes.logic.Unauthorized
import com.kutoru.mikunotes.logic.UnknownError
import com.kutoru.mikunotes.models.ResultBody
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.HttpStatement
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLParserException
import io.ktor.serialization.kotlinx.json.json
import java.net.ConnectException
import java.nio.channels.UnresolvedAddressException

class RequestManager(
    val context: Context,
) {

    lateinit var apiUrl: String
    lateinit var accessCookie: String
    lateinit var refreshCookie: String

    val notificationManager = NotificationManagerCompat.from(context)
    val persistentStorage = PersistentStorage(context)
    val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
    }

    var downloadIdx = 0

    init {
        updateUrl()
        updateCookies()
    }

    fun updateUrl() {
        val domain = persistentStorage.domain
        val port = persistentStorage.port ?: 0
        val isSecure = persistentStorage.isSecure

        val protocol = if (isSecure != null && isSecure) "https" else "http"
        apiUrl = "$protocol://$domain:$port"
    }

    fun updateCookies() {
        accessCookie = persistentStorage.accessCookie ?: ""
        refreshCookie = persistentStorage.refreshCookie ?: ""
    }

    suspend fun executeRequest(request: HttpStatement): HttpResponse {
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

    fun handleHttpStatus(status: HttpStatusCode) {
        println("Got a response with $status")
        when (status.value) {
            200, 201 -> {}
            401 -> throw Unauthorized()
            in 400..<500 -> throw BadRequest()
            500 -> throw ServerError()
            else -> throw UnknownError()
        }
    }

    fun <T>handleBody(body: ResultBody<T>): T {
        return body.data ?: throw ServerError()
    }

    fun notificationPermissionGranted(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true
        }

        return ActivityCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }
}
