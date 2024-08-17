package com.kutoru.mikunotes.ui.note

import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kutoru.mikunotes.logic.MikuNotesApp
import com.kutoru.mikunotes.logic.SELECTED_NOTE
import com.kutoru.mikunotes.logic.requests.RequestManager
import com.kutoru.mikunotes.models.File
import com.kutoru.mikunotes.models.Note
import com.kutoru.mikunotes.models.Tag
import com.kutoru.mikunotes.ui.ApiViewModel

class NoteViewModel(requestManager: RequestManager) : ApiViewModel(requestManager) {

    private val _text = MutableLiveData<String>()
    val text: LiveData<String> = _text

    private val _title = MutableLiveData<String>()
    val title: LiveData<String> = _title

    private val _created = MutableLiveData<Long>()
    val created: LiveData<Long> = _created

    private val _lastEdited = MutableLiveData<Long>()
    val lastEdited: LiveData<Long> = _lastEdited

    private val _timesEdited = MutableLiveData<Int>()
    val timesEdited: LiveData<Int> = _timesEdited

    private val _tags = MutableLiveData<List<Tag>>()
    val tags: LiveData<List<Tag>> = _tags

    private val _files = MutableLiveData<List<File>>()
    val files: LiveData<List<File>> = _files

    private lateinit var note: Note
    var initialized = false
        private set

    fun parseFromIntent(intent: Intent) {
        note = intent.getSerializableExtra(SELECTED_NOTE) as Note
        note.tags.addAll(listOf(
            Tag(1723018115, 1, "tag name 1", null, 1), Tag(1723018115, 2, "tag name 2", null, 1),
            Tag(1723018115, 3, "tag name 3", null, 1), Tag(1723018115, 4, "tag name 4", null, 1),
            Tag(1723018115, 5, "tag name 5", null, 1), Tag(1723018115, 6, "tag name 6", null, 1),
        ))

        _text.value = note.text
        _title.value = note.title
        _created.value = note.created
        _lastEdited.value = note.last_edited
        _timesEdited.value = note.times_edited
        _tags.value = note.tags
        _files.value = note.files

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
