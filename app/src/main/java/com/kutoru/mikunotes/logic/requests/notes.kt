package com.kutoru.mikunotes.logic.requests

import com.kutoru.mikunotes.models.Note
import com.kutoru.mikunotes.models.NoteGet
import com.kutoru.mikunotes.models.NotePost
import com.kutoru.mikunotes.models.NoteQueryParameters
import com.kutoru.mikunotes.models.NoteTagPost
import io.ktor.http.HttpMethod

suspend fun RequestManager.getNotes(queryParams: NoteQueryParameters): MutableList<Note> {
    val queryString = "?"
    return executeRequestUntilBody<NoteGet>("$apiUrl/notes$queryString", HttpMethod.Get).notes
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
