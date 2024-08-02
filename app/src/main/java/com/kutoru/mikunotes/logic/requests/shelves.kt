package com.kutoru.mikunotes.logic.requests

import com.kutoru.mikunotes.models.ResultBody
import com.kutoru.mikunotes.models.Shelf
import com.kutoru.mikunotes.models.ShelfPatch
import com.kutoru.mikunotes.models.ShelfToNote
import io.ktor.client.call.body
import io.ktor.client.request.prepareDelete
import io.ktor.client.request.prepareGet
import io.ktor.client.request.preparePatch
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

suspend fun RequestManager.getShelf(): Shelf {
    val url = "$apiUrl/shelf"
    val req = httpClient.prepareGet(url) {
        headers.append("Cookie", accessCookie)
    }

    val res = handleRequest(req)
    handleHttpStatus(res.status)
    val body: ResultBody<Shelf> = res.body()
    return handleBody(body)
}

suspend fun RequestManager.deleteShelf(): Shelf {
    val url = "$apiUrl/shelf"
    val req = httpClient.prepareDelete(url) {
        headers.append("Cookie", accessCookie)
    }

    val res = handleRequest(req)
    handleHttpStatus(res.status)
    val body: ResultBody<Shelf> = res.body()
    return handleBody(body)
}

suspend fun RequestManager.patchShelf(body: ShelfPatch): Shelf {
    val url = "$apiUrl/shelf"
    val req = httpClient.preparePatch(url) {
        headers.append("Cookie", accessCookie)
        contentType(ContentType.Application.Json)
        setBody(body)
    }

    val res = handleRequest(req)
    handleHttpStatus(res.status)
    val body: ResultBody<Shelf> = res.body()
    return handleBody(body)
}

suspend fun RequestManager.postShelfToNote(body: ShelfToNote): Shelf {
    val url = "$apiUrl/shelf/to-note"
    val req = httpClient.preparePost(url) {
        headers.append("Cookie", accessCookie)
        contentType(ContentType.Application.Json)
        setBody(body)
    }

    val res = handleRequest(req)
    handleHttpStatus(res.status)
    val body: ResultBody<Shelf> = res.body()
    return handleBody(body)
}
