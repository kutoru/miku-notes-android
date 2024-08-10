package com.kutoru.mikunotes.logic.requests

import com.kutoru.mikunotes.models.Note
import com.kutoru.mikunotes.models.NoteGet
import com.kutoru.mikunotes.models.NotePost
import com.kutoru.mikunotes.models.NoteQueryParameters
import com.kutoru.mikunotes.models.NoteTagPost
import io.ktor.http.HttpMethod
import java.net.URLEncoder

suspend fun RequestManager.getNotes(queryParams: NoteQueryParameters): Pair<MutableList<Note>, UInt> {
    var queryString = "?"

    if (queryParams.page != null) {
        queryString += "&page=${queryParams.page}"
    }
    if (queryParams.per_page != null) {
        queryString += "&per_page=${queryParams.per_page}"
    }
    if (queryParams.sort_by != null) {
        queryString += "&sort_by=${queryParams.sort_by}"
    }
    if (queryParams.sort_type != null) {
        queryString += "&sort_type=${queryParams.sort_type}"
    }
    if (queryParams.tags != null) {
        val tagsValue = queryParams.tags
            .map { it.toString() }
            .reduceOrNull { acc, v -> "$acc,$v" }
            .orEmpty()

        queryString += "&tags=$tagsValue"
    }
    if (queryParams.date != null) {
        queryString += "&date=${queryParams.date.first}-${queryParams.date.second}"
    }
    if (queryParams.date_modif != null) {
        queryString += "&date_modif=${queryParams.date_modif.first}-${queryParams.date_modif.second}"
    }
    if (queryParams.title != null) {
        queryString += "&title=${URLEncoder.encode(queryParams.title, "utf-8")}"
    }

    val result = executeRequestUntilBody<NoteGet>("$apiUrl/notes$queryString", HttpMethod.Get)
    return Pair(result.notes, result.total_count)
}

suspend fun RequestManager.postNotes(body: NotePost): Note {
    return executeRequestUntilBody("$apiUrl/notes", HttpMethod.Post, body)
}

suspend fun RequestManager.deleteNotes(noteId: Int) {
    return executeRequestUntilBody("$apiUrl/notes/$noteId", HttpMethod.Delete)
}

suspend fun RequestManager.patchNotes(noteId: Int, body: NotePost): Note {
    return executeRequestUntilBody("$apiUrl/notes/$noteId", HttpMethod.Patch, body)
}

suspend fun RequestManager.postNotesTag(noteId: Int, body: NoteTagPost): Note {
    return executeRequestUntilBody("$apiUrl/notes/$noteId/tag", HttpMethod.Patch, body)
}

suspend fun RequestManager.deleteNotesTag(noteId: Int, tagId: Int): Note {
    return executeRequestUntilBody("$apiUrl/notes/$noteId/tag/$tagId", HttpMethod.Patch)
}
