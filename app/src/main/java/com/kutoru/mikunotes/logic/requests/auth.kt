package com.kutoru.mikunotes.logic.requests

import com.kutoru.mikunotes.logic.ServerError
import com.kutoru.mikunotes.models.LoginBody
import io.ktor.client.request.prepareGet
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

suspend fun RequestManager.getAccess() {
    val url = "$apiUrl/access"
    val req = httpClient.prepareGet(url) {
        headers.append("Cookie", refreshCookie)
    }

    val res = executeRequest(req)
    handleHttpStatus(res.status)

    val accessCookie = res.headers["set-cookie"] ?: throw ServerError()
    persistentStorage.accessCookie = accessCookie

    updateCookies()
}

suspend fun RequestManager.postLogin(loginBody: LoginBody) {
    val url = "$apiUrl/login"
    val req = httpClient.preparePost(url) {
        contentType(ContentType.Application.Json)
        setBody(loginBody)
    }

    val res = executeRequest(req)
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

    persistentStorage.email = loginBody.email
    updateCookies()
}

suspend fun RequestManager.postRegister(loginBody: LoginBody) {
    val url = "$apiUrl/register"
    val req = httpClient.preparePost(url) {
        contentType(ContentType.Application.Json)
        setBody(loginBody)
    }

    val res = executeRequest(req)
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

    persistentStorage.email = loginBody.email
    updateCookies()
}

suspend fun RequestManager.getLogout() {
    val url = "$apiUrl/logout"
    val req = httpClient.prepareGet(url) {
        headers.append("Cookie", accessCookie)
    }

    val res = executeRequest(req)
    handleHttpStatus(res.status)

    println("Before: ${persistentStorage.accessCookie}; ${persistentStorage.refreshCookie}; ${persistentStorage.email}")

    persistentStorage.accessCookie = null
    persistentStorage.refreshCookie = null

    persistentStorage.email = null
    updateCookies()

    println("After: ${persistentStorage.accessCookie}; ${persistentStorage.refreshCookie}; ${persistentStorage.email}")
}
