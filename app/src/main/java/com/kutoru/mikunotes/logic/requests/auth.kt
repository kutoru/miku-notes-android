package com.kutoru.mikunotes.logic.requests

import com.kutoru.mikunotes.logic.ServerError
import com.kutoru.mikunotes.models.LoginBody
import io.ktor.http.HttpMethod

suspend fun RequestManager.getAccess() {
    val res = executeRequestUntilResponse("$apiUrl/access", HttpMethod.Get)

    val accessCookie = res.headers["set-cookie"] ?: throw ServerError("Did not get the access cookie")
    persistentStorage.accessCookie = accessCookie

    updateCookies()
}

suspend fun RequestManager.postLogin(body: LoginBody) {
    val res = executeRequestUntilResponse("$apiUrl/login", HttpMethod.Post, body)

    val rawCookies = res.headers.getAll("set-cookie")
    if (rawCookies == null || rawCookies.size != 2) {
        throw ServerError("Got invalid cookies")
    }

    val cookies = rawCookies.map { it.split(';')[0] }

    if (cookies[0].startsWith("at=")) {
        persistentStorage.accessCookie = cookies[0]
        persistentStorage.refreshCookie = cookies[1]
    } else {
        persistentStorage.accessCookie = cookies[1]
        persistentStorage.refreshCookie = cookies[0]
    }

    persistentStorage.email = body.email
    updateCookies()
}

suspend fun RequestManager.postRegister(body: LoginBody) {
    val res = executeRequestUntilResponse("$apiUrl/register", HttpMethod.Post, body)

    val rawCookies = res.headers.getAll("set-cookie")
    if (rawCookies == null || rawCookies.size != 2) {
        throw ServerError("Got invalid cookies")
    }

    val cookies = rawCookies.map { it.split(';')[0] }

    if (cookies[0].startsWith("at=")) {
        persistentStorage.accessCookie = cookies[0]
        persistentStorage.refreshCookie = cookies[1]
    } else {
        persistentStorage.accessCookie = cookies[1]
        persistentStorage.refreshCookie = cookies[0]
    }

    persistentStorage.email = body.email
    updateCookies()
}

suspend fun RequestManager.getLogout() {
    executeRequestUntilResponse("$apiUrl/logout", HttpMethod.Get)

    persistentStorage.accessCookie = null
    persistentStorage.refreshCookie = null

    persistentStorage.email = null
    updateCookies()
}
