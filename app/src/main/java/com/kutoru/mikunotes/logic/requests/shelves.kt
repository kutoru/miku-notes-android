package com.kutoru.mikunotes.logic.requests

import com.kutoru.mikunotes.models.Shelf
import com.kutoru.mikunotes.models.ShelfPatch
import com.kutoru.mikunotes.models.ShelfToNote
import io.ktor.http.HttpMethod

suspend fun RequestManager.getShelf(): Shelf {
    return executeRequestUntilBody("$apiUrl/shelf", HttpMethod.Get)
}

suspend fun RequestManager.deleteShelf(): Shelf {
    return executeRequestUntilBody("$apiUrl/shelf", HttpMethod.Delete)
}

suspend fun RequestManager.patchShelf(body: ShelfPatch): Shelf {
    return executeRequestUntilBody("$apiUrl/shelf", HttpMethod.Patch, body)
}

suspend fun RequestManager.postShelfToNote(body: ShelfToNote): Shelf {
    return executeRequestUntilBody("$apiUrl/shelf/to-note", HttpMethod.Post, body)
}
