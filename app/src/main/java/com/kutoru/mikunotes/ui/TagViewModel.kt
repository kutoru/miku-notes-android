package com.kutoru.mikunotes.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kutoru.mikunotes.logic.MikuNotesApp
import com.kutoru.mikunotes.logic.requests.RequestManager
import com.kutoru.mikunotes.logic.requests.deleteTags
import com.kutoru.mikunotes.logic.requests.getTags
import com.kutoru.mikunotes.logic.requests.patchTags
import com.kutoru.mikunotes.logic.requests.postTags
import com.kutoru.mikunotes.models.Tag
import com.kutoru.mikunotes.models.TagPost

class TagViewModel(requestManager: RequestManager) : ApiViewModel(requestManager) {

    private val _tags = MutableLiveData<MutableList<Tag>>(mutableListOf())
    val tags: LiveData<MutableList<Tag>> = _tags

    var initialized = false
        private set

    suspend fun getTags() {
        initialized = false
        _tags.value = requestManager.getTags()

//        val temp = requestManager.getTags()

//        _tags.value = mutableListOf()

//        _tags.value = mutableListOf(
//            Tag(1723018115, 1, "tag name 1", null, 1), Tag(1723018115, 2, "tag name 2", null, 1),
//            Tag(1723018115, 3, "tag name askldfjas 3", null, 1), Tag(1723018115, 4, "tag name 4", null, 1),
//            Tag(1723018115, 5, "tag name 5", null, 1), Tag(1723018115, 6, "tag name 6", null, 1),
//            Tag(1723018115, 7, "tag name alsdfjklasldfke 7", null, 1), Tag(1723018115, 8, "tag name 8", null, 1),
//            Tag(1723018115, 9, "tag name 9", null, 1), Tag(1723018115, 10, "tag name asdklfjalskdjlasdj 10", null, 1),
//            Tag(1723018115, 11, "tag name 11", null, 1), Tag(1723018115, 12, "tag name alskdjfla 12", null, 1),
//            Tag(1723018115, 13, "tag name aklsjdlasdfjaslkdfklasjdf 13", null, 1), Tag(1723018115, 14, "tag name 14", null, 1),
//            Tag(1723018115, 15, "tag name 15", null, 1), Tag(1723018115, 16, "tag name 16", null, 1),
//            Tag(1723018115, 17, "tag name 1", null, 1), Tag(1723018115, 18, "tag name 2", null, 1),
//            Tag(1723018115, 19, "tag name askldfjas 3", null, 1), Tag(1723018115, 20, "tag name 4", null, 1),
//            Tag(1723018115, 21, "tag name 5", null, 1), Tag(1723018115, 22, "tag name 6", null, 1),
//        )

        initialized = true
    }

    suspend fun postTags(tagName: String) {
        val tag = requestManager.postTags(TagPost(tagName))
        _tags.value!!.add(tag)
        _tags.value = _tags.value
    }

    suspend fun deleteTags(tagId: Int) {
        requestManager.deleteTags(tagId)

        val index = _tags.value!!.indexOfFirst { it.id == tagId }
        if (index == -1) {
            throw Exception("Could not find tag's index")
        }

        _tags.value!!.removeAt(index)
        _tags.value = _tags.value
    }

    suspend fun patchTags(tagId: Int, newName: String) {
        val tag = requestManager.patchTags(tagId, TagPost(newName))

        val index = _tags.value!!.indexOfFirst { it.id == tagId }
        if (index == -1) {
            throw Exception("Could not find tag's index")
        }

        _tags.value!![index] = tag
        _tags.value = _tags.value
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
