package com.kutoru.mikunotes.ui

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kutoru.mikunotes.logic.MikuNotesApp
import com.kutoru.mikunotes.logic.requests.RequestManager
import com.kutoru.mikunotes.logic.requests.deleteFile
import com.kutoru.mikunotes.logic.requests.getFile
import com.kutoru.mikunotes.logic.requests.postFileToNote
import com.kutoru.mikunotes.logic.requests.postFileToShelf
import com.kutoru.mikunotes.models.File

class FileViewModel(requestManager: RequestManager) : ApiViewModel(requestManager) {

    suspend fun postFileToShelf(contentResolver: ContentResolver, fileUri: Uri, shelfId: Int): File {
        return requestManager.postFileToShelf(contentResolver, fileUri, shelfId)
    }

    suspend fun postFileToNote(contentResolver: ContentResolver, fileUri: Uri, noteId: Int): File {
        return requestManager.postFileToNote(contentResolver, fileUri, noteId)
    }

    suspend fun getFile(fileHash: String) {
        requestManager.getFile(fileHash)
    }

    suspend fun deleteFile(fileId: Int) {
        requestManager.deleteFile(fileId)
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val requestManager = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MikuNotesApp).requestManager
                FileViewModel(requestManager)
            }
        }
    }
}
