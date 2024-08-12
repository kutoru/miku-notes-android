package com.kutoru.mikunotes.viewmodels

import android.content.Intent
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kutoru.mikunotes.logic.MikuNotesApp
import com.kutoru.mikunotes.logic.SELECTED_NOTE
import com.kutoru.mikunotes.logic.requests.RequestManager
import com.kutoru.mikunotes.models.Note

class NoteViewModel(requestManager: RequestManager) : ApiViewModel(requestManager) {

    lateinit var note: Note
        private set
    var initialized = false
        private set

    fun parseFromIntent(intent: Intent) {
        note = intent.getSerializableExtra(SELECTED_NOTE) as Note
        initialized = true
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val requestManager = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MikuNotesApp).requestManager
                NoteViewModel(requestManager)
            }
        }
    }
}
