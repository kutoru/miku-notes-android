package com.kutoru.mikunotes.logic.requests

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.kutoru.mikunotes.logic.BadRequest
import com.kutoru.mikunotes.logic.InvalidUrl
import com.kutoru.mikunotes.logic.PersistentStorage
import com.kutoru.mikunotes.logic.ServerError
import com.kutoru.mikunotes.logic.Unauthorized
import com.kutoru.mikunotes.models.ResultBody
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.prepareRequest
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.HttpStatement
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.URLParserException
import io.ktor.http.contentType
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

    var notificationIndex = 0

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

    suspend inline fun <reified T>buildRequest(url: String, method: HttpMethod, body: T?): HttpStatement {
        return httpClient.prepareRequest(url) {
            this.method = method
            headers.append("Cookie", refreshCookie)
            headers.append("Cookie", accessCookie)

            if (body != null) {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        }
    }

    suspend fun executeRequestUntilResponse(request: HttpStatement): HttpResponse {
        val response = try {
            request.execute()
        } catch (e: Exception) {
            throw when (e) {
                is ConnectException,
                is UnresolvedAddressException,
                is URLParserException,
                is IllegalArgumentException -> InvalidUrl(e.toString())
                else -> e
            }
        }

        println("Got a response with ${response.status}")

        when (response.status.value) {
            200, 201 -> return response
            401 -> throw Unauthorized()
            in 400..<500 -> throw BadRequest(response.status.toString())
            else -> throw ServerError(response.status.toString())
        }
    }

    suspend inline fun executeRequestUntilResponse(url: String, method: HttpMethod): HttpResponse {
        val request = buildRequest<Unit>(url, method, null)
        return executeRequestUntilResponse(request)
    }

    suspend inline fun <reified T>executeRequestUntilResponse(url: String, method: HttpMethod, body: T): HttpResponse {
        val request = buildRequest(url, method, body)
        return executeRequestUntilResponse(request)
    }

    suspend inline fun <reified U>executeRequestUntilBody(url: String, method: HttpMethod): U {
        val request = buildRequest<Unit>(url, method, null)
        return executeRequestUntilBody(request)
    }

    suspend inline fun <reified T, reified U>executeRequestUntilBody(url: String, method: HttpMethod, body: T): U {
        val request = buildRequest(url, method, body)
        return executeRequestUntilBody(request)
    }

    suspend inline fun <reified U>executeRequestUntilBody(request: HttpStatement): U {
        val response = executeRequestUntilResponse(request)
        val body: ResultBody<U> = response.body()
        return body.data ?: throw ServerError("The data field is invalid in the response body")
    }
}
