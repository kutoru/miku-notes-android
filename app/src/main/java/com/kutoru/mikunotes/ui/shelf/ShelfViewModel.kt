package com.kutoru.mikunotes.ui.shelf

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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
import com.kutoru.mikunotes.models.File
import com.kutoru.mikunotes.models.Shelf
import com.kutoru.mikunotes.models.ShelfPatch
import com.kutoru.mikunotes.models.ShelfToNote
import com.kutoru.mikunotes.ui.ApiViewModel

class ShelfViewModel(requestManager: RequestManager) : ApiViewModel(requestManager) {

    private val _text = MutableLiveData<String>()
    val text: LiveData<String> = _text

    private val _lastEdited = MutableLiveData<Long>()
    val lastEdited: LiveData<Long> = _lastEdited

    private val _timesEdited = MutableLiveData<Int>()
    val timesEdited: LiveData<Int> = _timesEdited

    private val _files = MutableLiveData<List<File>>()
    val files: LiveData<List<File>> = _files

    private lateinit var shelf: Shelf
    var initialized = false
        private set

    suspend fun deleteFile(fileIndex: Int) {
        val fileId = shelf.files[fileIndex].id
        requestManager.deleteFile(fileId)
        shelf.files.removeAt(fileIndex)

        _files.value = shelf.files
    }

    suspend fun getFile(fileIndex: Int) {
        val fileHash = shelf.files[fileIndex].hash
        requestManager.getFile(fileHash)
    }

    suspend fun postFile(contentResolver: ContentResolver, fileUri: Uri) {
        val file = requestManager.postFileToShelf(contentResolver, fileUri, shelf.id)
        shelf.files.add(file)

        _files.value = shelf.files
    }

    suspend fun getShelf() {
        shelf = requestManager.getShelf()

        _text.value = shelf.text
        _lastEdited.value = shelf.last_edited
        _timesEdited.value = shelf.times_edited
        _files.value = shelf.files

        initialized = true
    }

    suspend fun patchShelf(newText: String) {
        shelf = requestManager.patchShelf(ShelfPatch(newText))

        _text.value = shelf.text
        _lastEdited.value = shelf.last_edited
        _timesEdited.value = shelf.times_edited
    }

    suspend fun deleteShelf() {
        shelf = requestManager.deleteShelf()

        _text.value = shelf.text
        _lastEdited.value = shelf.last_edited
        _timesEdited.value = shelf.times_edited
        _files.value = shelf.files
    }

    suspend fun postShelfToNote(noteTitle: String) {
        shelf = requestManager.postShelfToNote(ShelfToNote(noteTitle, shelf.text))

        _text.value = shelf.text
        _lastEdited.value = shelf.last_edited
        _timesEdited.value = shelf.times_edited
        _files.value = shelf.files
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
