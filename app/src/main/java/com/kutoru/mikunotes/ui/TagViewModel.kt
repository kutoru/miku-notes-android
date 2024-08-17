package com.kutoru.mikunotes.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kutoru.mikunotes.logic.MikuNotesApp
import com.kutoru.mikunotes.logic.requests.RequestManager
import com.kutoru.mikunotes.logic.requests.getTags
import com.kutoru.mikunotes.models.Tag

class TagViewModel(requestManager: RequestManager) : ApiViewModel(requestManager) {

    private val _tags = MutableLiveData<List<Tag>>()
    val tags: LiveData<List<Tag>> = _tags

    suspend fun getTags() {
//        _tags.value = requestManager.getTags()

        val temp = requestManager.getTags()
        _tags.value = mutableListOf(
            Tag(1723018115, 1, "tag name 1", null, 1), Tag(1723018115, 2, "tag name 2", null, 1),
            Tag(1723018115, 3, "tag name 3", null, 1), Tag(1723018115, 4, "tag name 4", null, 1),
            Tag(1723018115, 5, "tag name 5", null, 1), Tag(1723018115, 6, "tag name 6", null, 1),
            Tag(1723018115, 7, "tag name 7", null, 1), Tag(1723018115, 8, "tag name 8", null, 1),
            Tag(1723018115, 9, "tag name 9", null, 1), Tag(1723018115, 10, "tag name 10", null, 1),
            Tag(1723018115, 11, "tag name 11", null, 1), Tag(1723018115, 12, "tag name 12", null, 1),
            Tag(1723018115, 13, "tag name 13", null, 1), Tag(1723018115, 14, "tag name 14", null, 1),
            Tag(1723018115, 15, "tag name 15", null, 1), Tag(1723018115, 16, "tag name 16", null, 1),
        )

    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val requestManager = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MikuNotesApp).requestManager
                TagViewModel(requestManager)
            }
        }
    }
}
