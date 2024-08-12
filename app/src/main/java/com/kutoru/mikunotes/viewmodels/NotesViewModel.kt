package com.kutoru.mikunotes.viewmodels

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kutoru.mikunotes.logic.MikuNotesApp
import com.kutoru.mikunotes.logic.requests.RequestManager
import com.kutoru.mikunotes.logic.requests.getNotes
import com.kutoru.mikunotes.models.Note
import com.kutoru.mikunotes.models.NoteQueryParameters

class NotesViewModel(requestManager: RequestManager) : ApiViewModel(requestManager) {

    var notes = mutableListOf<Note>()
        private set
    var pageCount = 0u
        private set
    var initialized = false
        private set

    suspend fun getNotes(parameters: NoteQueryParameters) {
        val (newNotes, newPageCount) = requestManager.getNotes(parameters)
        notes = newNotes
        pageCount = newPageCount
        initialized = true
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
