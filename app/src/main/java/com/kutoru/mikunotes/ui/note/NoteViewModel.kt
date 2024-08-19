package com.kutoru.mikunotes.ui.note

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kutoru.mikunotes.logic.CREATE_NEW_NOTE
import com.kutoru.mikunotes.logic.MikuNotesApp
import com.kutoru.mikunotes.logic.SELECTED_NOTE
import com.kutoru.mikunotes.logic.requests.RequestManager
import com.kutoru.mikunotes.logic.requests.deleteFile
import com.kutoru.mikunotes.logic.requests.deleteNotesTag
import com.kutoru.mikunotes.logic.requests.getFile
import com.kutoru.mikunotes.logic.requests.postFileToNote
import com.kutoru.mikunotes.logic.requests.postNotesTag
import com.kutoru.mikunotes.models.File
import com.kutoru.mikunotes.models.Note
import com.kutoru.mikunotes.models.NoteTagPost
import com.kutoru.mikunotes.models.Tag
import com.kutoru.mikunotes.ui.ApiViewModel

class NoteViewModel(requestManager: RequestManager) : ApiViewModel(requestManager) {

    private var noteId = 0

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

    private val _files = MutableLiveData<MutableList<File>>(mutableListOf())
    val files: LiveData<MutableList<File>> = _files

    private val _isNewNote = MutableLiveData(false)
    val isNewNote: LiveData<Boolean> = _isNewNote

    var initialized = false
        private set

    fun parseFromIntent(intent: Intent) {
        if (intent.getBooleanExtra(CREATE_NEW_NOTE, false)) {
            _isNewNote.value = true
            initialized = true
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

    suspend fun deleteFile(fileIndex: Int) {
        val fileId = _files.value!![fileIndex].id
        requestManager.deleteFile(fileId)

        _files.value!!.removeAt(fileIndex)
        _files.value = _files.value
    }

    suspend fun getFile(fileIndex: Int) {
        val fileHash = _files.value!![fileIndex].hash
        requestManager.getFile(fileHash)
    }

    suspend fun postFile(contentResolver: ContentResolver, fileUri: Uri) {
        val file = requestManager.postFileToNote(contentResolver, fileUri, noteId)

        _files.value!!.add(file)
        _files.value = _files.value
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

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val requestManager = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MikuNotesApp).requestManager
                NoteViewModel(requestManager)
            }
        }
    }
}
