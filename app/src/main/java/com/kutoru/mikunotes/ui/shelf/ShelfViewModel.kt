package com.kutoru.mikunotes.ui.shelf

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kutoru.mikunotes.logic.MikuNotesApp
import com.kutoru.mikunotes.logic.requests.RequestManager
import com.kutoru.mikunotes.logic.requests.deleteShelf
import com.kutoru.mikunotes.logic.requests.getShelf
import com.kutoru.mikunotes.logic.requests.patchShelf
import com.kutoru.mikunotes.logic.requests.postShelfToNote
import com.kutoru.mikunotes.models.ShelfPatch
import com.kutoru.mikunotes.models.ShelfToNote
import com.kutoru.mikunotes.ui.FileHoldingViewModel

class ShelfViewModel(requestManager: RequestManager) : FileHoldingViewModel(requestManager) {

    private var shelfId = 0

    override val isAttachedToShelf = true
    override val itemId get() = shelfId

    private val _text = MutableLiveData<String>()
    val text: LiveData<String> = _text

    private val _lastEdited = MutableLiveData<Long>()
    val lastEdited: LiveData<Long> = _lastEdited

    private val _timesEdited = MutableLiveData<Int>()
    val timesEdited: LiveData<Int> = _timesEdited

    var initialized = false
        private set

    suspend fun getShelf() {
        val shelf = requestManager.getShelf()

        shelfId = shelf.id
        _text.value = shelf.text
        _lastEdited.value = shelf.last_edited
        _timesEdited.value = shelf.times_edited
        _files.value = shelf.files

        initialized = true
    }

    suspend fun patchShelf(newText: String) {
        val shelf = requestManager.patchShelf(ShelfPatch(newText))

        _text.value = shelf.text
        _lastEdited.value = shelf.last_edited
        _timesEdited.value = shelf.times_edited
    }

    suspend fun deleteShelf() {
        val shelf = requestManager.deleteShelf()

        _text.value = shelf.text
        _lastEdited.value = shelf.last_edited
        _timesEdited.value = shelf.times_edited
        _files.value = shelf.files
    }

    suspend fun postShelfToNote(noteTitle: String, noteText: String) {
        val shelf = requestManager.postShelfToNote(
            ShelfToNote(noteTitle, noteText),
        )

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
