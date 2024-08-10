package com.kutoru.mikunotes.logic.requests

import com.kutoru.mikunotes.models.Tag
import com.kutoru.mikunotes.models.TagGet
import com.kutoru.mikunotes.models.TagPost
import io.ktor.http.HttpMethod

suspend fun RequestManager.getTags(): MutableList<Tag> {
    return executeRequestUntilBody<TagGet>("$apiUrl/tags", HttpMethod.Get).tags
}

suspend fun RequestManager.postTags(body: TagPost): Tag {
    return executeRequestUntilBody("$apiUrl/tags", HttpMethod.Post, body)
}

suspend fun RequestManager.deleteTags(tagId: Int) {
    return executeRequestUntilBody("$apiUrl/tags/$tagId", HttpMethod.Delete)
}

suspend fun RequestManager.patchTags(tagId: Int, body: TagPost): Tag {
    return executeRequestUntilBody("$apiUrl/tags/$tagId", HttpMethod.Patch, body)
}
