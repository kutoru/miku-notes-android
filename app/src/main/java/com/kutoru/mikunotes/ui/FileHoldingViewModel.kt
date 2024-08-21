package com.kutoru.mikunotes.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.kutoru.mikunotes.logic.requests.RequestManager
import com.kutoru.mikunotes.models.File

abstract class FileHoldingViewModel(
    requestManager: RequestManager,
) : ApiViewModel(requestManager) {

    abstract val isAttachedToShelf: Boolean
    abstract val itemId: Int

    protected val _files = MutableLiveData<MutableList<File>>(mutableListOf())
    val files: LiveData<MutableList<File>> = _files

    fun fileDeleted(fileId: Int) {
        val prevSize = _files.value!!.size
        _files.value!!.removeIf { it.id == fileId }

        if (_files.value!!.size != prevSize) {
            _files.value = _files.value
        }
    }

    fun fileUploaded(file: File) {
        if (
            (file.attach_id == 0 && isAttachedToShelf) ||
            (file.attach_id == itemId && !isAttachedToShelf)
        ) {
            _files.value!!.add(file)
            _files.value = _files.value
        }
    }
}
