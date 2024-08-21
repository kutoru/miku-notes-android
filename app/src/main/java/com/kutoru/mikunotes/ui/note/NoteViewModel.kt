package com.kutoru.mikunotes.ui.note

import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kutoru.mikunotes.logic.CREATE_NEW_NOTE
import com.kutoru.mikunotes.logic.MikuNotesApp
import com.kutoru.mikunotes.logic.SELECTED_NOTE
import com.kutoru.mikunotes.logic.requests.RequestManager
import com.kutoru.mikunotes.logic.requests.deleteNotes
import com.kutoru.mikunotes.logic.requests.deleteNotesTag
import com.kutoru.mikunotes.logic.requests.getNotes
import com.kutoru.mikunotes.logic.requests.patchNotes
import com.kutoru.mikunotes.logic.requests.postNotes
import com.kutoru.mikunotes.logic.requests.postNotesTag
import com.kutoru.mikunotes.models.Note
import com.kutoru.mikunotes.models.NotePost
import com.kutoru.mikunotes.models.NoteQueryParameters
import com.kutoru.mikunotes.models.NoteTagPost
import com.kutoru.mikunotes.models.Tag
import com.kutoru.mikunotes.ui.FileHoldingViewModel

class NoteViewModel(requestManager: RequestManager) : FileHoldingViewModel(requestManager) {

    private var noteId = 0

    override val isAttachedToShelf = false
    override val itemId get() = noteId

    private val _text = MutableLiveData("")
    val text: LiveData<String> = _text

    private val _title = MutableLiveData("")
    val title: LiveData<String> = _title

    private val _created = MutableLiveData<Long?>(null)
    val created: LiveData<Long?> = _created

    private val _lastEdited = MutableLiveData<Long?>(null)
    val lastEdited: LiveData<Long?> = _lastEdited

    private val _timesEdited = MutableLiveData<Int?>(null)
    val timesEdited: LiveData<Int?> = _timesEdited

    private val _tags = MutableLiveData<MutableList<Tag>>(mutableListOf())
    val tags: LiveData<MutableList<Tag>> = _tags

    private val _isNewNote = MutableLiveData(false)
    val isNewNote: LiveData<Boolean> = _isNewNote

    var initialized = false
        private set

    fun parseFromIntent(intent: Intent) {
        if (intent.getBooleanExtra(CREATE_NEW_NOTE, false)) {
            _isNewNote.value = true
            return
        }

        val note = intent.getSerializableExtra(SELECTED_NOTE) as Note

        noteId = note.id
        _text.value = note.text
        _title.value = note.title
        _created.value = note.created
        _lastEdited.value = note.last_edited
        _timesEdited.value = note.times_edited
        _tags.value = note.tags
        _files.value = note.files

        initialized = true
    }

    suspend fun postNotesTag(tag: Tag) {
        requestManager.postNotesTag(noteId, NoteTagPost(tag.id))

        _tags.value!!.add(tag)
        _tags.value = _tags.value
    }

    suspend fun deleteNotesTag(tagIndex: Int) {
        val tagId = _tags.value!![tagIndex].id
        requestManager.deleteNotesTag(noteId, tagId)

        _tags.value!!.removeAt(tagIndex)
        _tags.value = _tags.value
    }

    fun tagRemoved(tagIndex: Int) {
        _tags.value!!.removeAt(tagIndex)
        _tags.value = _tags.value
    }

    fun tagUpdated(tagIndex: Int, newName: String) {
        _tags.value!![tagIndex].name = newName
        _tags.value = _tags.value
    }

    suspend fun getNote() {
        // todo: implement a "get note by id" route in the backend lmao
        val note = requestManager.getNotes(NoteQueryParameters(
            null, 1u, null, null, null, Pair(_created.value!!, _created.value!!), null, null,
        )).first.first()

        noteId = note.id
        _text.value = note.text
        _title.value = note.title
        _created.value = note.created
        _lastEdited.value = note.last_edited
        _timesEdited.value = note.times_edited
        _tags.value = note.tags
        _files.value = note.files
    }

    suspend fun postNote(text: String, title: String) {
        val note = requestManager.postNotes(NotePost(text, title))

        noteId = note.id
        _text.value = note.text
        _title.value = note.title
        _created.value = note.created
        _lastEdited.value = note.last_edited
        _timesEdited.value = note.times_edited
        _tags.value = note.tags
        _files.value = note.files

        initialized = true
        _isNewNote.value = false
    }

    suspend fun deleteNote() {
        requestManager.deleteNotes(noteId)
        initialized = false
    }

    suspend fun patchNote(text: String, title: String) {
        val note = requestManager.patchNotes(noteId, NotePost(text, title))

        // todo: make backend return attached tags and files on patchNotes
        _text.value = note.text
        _title.value = note.title
        _created.value = note.created
        _lastEdited.value = note.last_edited
        _timesEdited.value = note.times_edited
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
