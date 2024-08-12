package com.kutoru.mikunotes.viewmodels

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kutoru.mikunotes.logic.MikuNotesApp
import com.kutoru.mikunotes.logic.requests.RequestManager
import com.kutoru.mikunotes.logic.requests.deleteFile
import com.kutoru.mikunotes.logic.requests.deleteShelf
import com.kutoru.mikunotes.logic.requests.getFile
import com.kutoru.mikunotes.logic.requests.getShelf
import com.kutoru.mikunotes.logic.requests.patchShelf
import com.kutoru.mikunotes.logic.requests.postFileToShelf
import com.kutoru.mikunotes.logic.requests.postShelfToNote
import com.kutoru.mikunotes.models.Shelf
import com.kutoru.mikunotes.models.ShelfPatch
import com.kutoru.mikunotes.models.ShelfToNote

class ShelfViewModel(requestManager: RequestManager) : ApiViewModel(requestManager) {

    lateinit var shelf: Shelf
        private set
    var initialized = false
        private set

    suspend fun deleteFile(fileIndex: Int) {
        val fileId = shelf.files[fileIndex].id
        requestManager.deleteFile(fileId)
        shelf.files.removeAt(fileIndex)
    }

    suspend fun getFile(fileIndex: Int) {
        val fileHash = shelf.files[fileIndex].hash
        requestManager.getFile(fileHash)
    }

    suspend fun postFile(contentResolver: ContentResolver, fileUri: Uri) {
        val file = requestManager.postFileToShelf(contentResolver, fileUri, shelf.id)
        shelf.files.add(file)
    }

    suspend fun getShelf() {
        shelf = requestManager.getShelf()
        initialized = true
    }

    suspend fun patchShelf(newText: String) {
        shelf = requestManager.patchShelf(ShelfPatch(newText))
    }

    suspend fun deleteShelf() {
        shelf = requestManager.deleteShelf()
    }

    suspend fun postShelfToNote(noteTitle: String) {
        shelf = requestManager.postShelfToNote(ShelfToNote(noteTitle, shelf.text))
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val requestManager = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MikuNotesApp).requestManager
                ShelfViewModel(requestManager)
            }
        }
    }
}
