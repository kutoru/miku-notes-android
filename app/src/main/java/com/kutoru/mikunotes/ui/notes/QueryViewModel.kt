package com.kutoru.mikunotes.ui.notes

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kutoru.mikunotes.models.NoteQueryParameters
import com.kutoru.mikunotes.models.QuerySortBy
import com.kutoru.mikunotes.models.QuerySortType

class QueryViewModel : ViewModel() {

    private val _page = MutableLiveData<UShort?>(null)
    val page: LiveData<UShort?> = _page

    private val _perPage = MutableLiveData<UByte?>(100u)
    val perPage: LiveData<UByte?> = _perPage

    private val _sortBy = MutableLiveData<QuerySortBy?>(null)
    val sortBy: LiveData<QuerySortBy?> = _sortBy

    private val _sortType = MutableLiveData<QuerySortType?>(null)
    val sortType: LiveData<QuerySortType?> = _sortType

    private val _tags = MutableLiveData<MutableSet<Int>?>(null)
    val tags: LiveData<MutableSet<Int>?> = _tags

    private val _date = MutableLiveData<Pair<Long, Long>>(Pair(0, 0))
    val date: LiveData<Pair<Long, Long>> = _date

    private val _dateModified = MutableLiveData<Pair<Long, Long>>(Pair(0, 0))
    val dateModified: LiveData<Pair<Long, Long>> = _dateModified

    private val _title = MutableLiveData<String?>(null)
    val title: LiveData<String?> = _title

    val queryParameters get() = NoteQueryParameters(
        page.value,
        perPage.value,
        sortBy.value,
        sortType.value,
        prepareTags(),
        date.value,
        dateModified.value,
        title.value,
    )

    fun addTag(tagId: Int) {
        if (tagId == 0) {
            _tags.value = mutableSetOf(0)
        } else if (_tags.value == null) {
            _tags.value = mutableSetOf(tagId)
        } else {
            _tags.value!!.remove(0)
            _tags.value!!.add(tagId)
            _tags.value = _tags.value
        }
    }

    fun removeTag(tagId: Int) {
        if (_tags.value == null) {
            return
        }

        _tags.value!!.remove(tagId)
        if (_tags.value!!.isNotEmpty()) {
            _tags.value = _tags.value
        } else {
            _tags.value = null
        }
    }

    fun setDateStart(timestampInSeconds: Long) {
        _date.value = Pair(timestampInSeconds, _date.value!!.second)
    }

    fun setDateEnd(timestampInSeconds: Long) {
        _date.value = Pair(_date.value!!.first, timestampInSeconds)
    }

    fun setDateModifiedStart(timestampInSeconds: Long) {
        _dateModified.value = Pair(timestampInSeconds, _dateModified.value!!.second)
    }

    fun setDateModifiedEnd(timestampInSeconds: Long) {
        _dateModified.value = Pair(_dateModified.value!!.first, timestampInSeconds)
    }

    fun setTitle(title: String?) {
        _title.value = if (title.isNullOrEmpty()) null else title
    }

    fun clearQuery() {
//        _page.value = null
//        _perPage.value = null
        _sortBy.value = null
        _sortType.value = null
        _tags.value = null
        _date.value = Pair(0, 0)
        _dateModified.value = Pair(0, 0)
        _title.value = null
    }

    private fun prepareTags(): Set<Int>? {
        return if (!_tags.value.isNullOrEmpty() && _tags.value!!.contains(0)) {
            emptySet()
        } else {
            _tags.value
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                QueryViewModel()
            }
        }
    }
}
