package com.kutoru.mikunotes.logic.requests

import com.kutoru.mikunotes.models.ResultBody
import com.kutoru.mikunotes.models.Tag
import com.kutoru.mikunotes.models.TagGet
import com.kutoru.mikunotes.models.TagPost
import io.ktor.client.call.body
import io.ktor.client.request.prepareDelete
import io.ktor.client.request.prepareGet
import io.ktor.client.request.preparePatch
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

suspend fun RequestManager.getTags(): MutableList<Tag> {
    val url = "$apiUrl/tags"
    val req = httpClient.prepareGet(url) {
        headers.append("Cookie", accessCookie)
    }

    val res = executeRequest(req)
    handleHttpStatus(res.status)
    val body: ResultBody<TagGet> = res.body()
    return handleBody(body).tags
}

suspend fun RequestManager.postTags(body: TagPost): Tag {
    val url = "$apiUrl/tags"
    val req = httpClient.preparePost(url) {
        headers.append("Cookie", accessCookie)
        contentType(ContentType.Application.Json)
        setBody(body)
    }

    val res = executeRequest(req)
    handleHttpStatus(res.status)
    val body: ResultBody<Tag> = res.body()
    return handleBody(body)
}

suspend fun RequestManager.deleteTags(tagId: Int) {
    val url = "$apiUrl/tags/$tagId"
    val req = httpClient.prepareDelete(url) {
        headers.append("Cookie", accessCookie)
    }

    val res = executeRequest(req)
    handleHttpStatus(res.status)
    val body: ResultBody<Unit> = res.body()
    return handleBody(body)
}

suspend fun RequestManager.patchTags(body: TagPost): Tag {
    val url = "$apiUrl/tags"
    val req = httpClient.preparePatch(url) {
        headers.append("Cookie", accessCookie)
        contentType(ContentType.Application.Json)
        setBody(body)
    }

    val res = executeRequest(req)
    handleHttpStatus(res.status)
    val body: ResultBody<Tag> = res.body()
    return handleBody(body)
}
