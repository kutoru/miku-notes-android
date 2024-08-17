package com.kutoru.mikunotes.ui.notes

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kutoru.mikunotes.logic.MikuNotesApp
import com.kutoru.mikunotes.logic.requests.RequestManager
import com.kutoru.mikunotes.logic.requests.getNotes
import com.kutoru.mikunotes.models.Note
import com.kutoru.mikunotes.models.NoteQueryParameters
import com.kutoru.mikunotes.ui.ApiViewModel

class NotesViewModel(requestManager: RequestManager) : ApiViewModel(requestManager) {

    private val _notes = MutableLiveData<List<Note>?>(null)
    val notes: LiveData<List<Note>?> = _notes

    private val _pageCount = MutableLiveData<UInt>()
    val pageCount: LiveData<UInt> = _pageCount

    suspend fun getNotes(parameters: NoteQueryParameters) {
        _notes.value = null
        val (newNotes, newPageCount) = requestManager.getNotes(parameters)

        _notes.value = newNotes
        _pageCount.value = newPageCount
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val requestManager = (this[APPLICATION_KEY] as MikuNotesApp).requestManager
                NotesViewModel(requestManager)
            }
        }
    }
}
