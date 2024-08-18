package com.kutoru.mikunotes.ui.notes

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kutoru.mikunotes.models.NoteQueryParameters
import com.kutoru.mikunotes.models.QuerySortBy
import com.kutoru.mikunotes.models.QuerySortType

private interface DefaultValues {
    companion object {
        val page: UShort? = null
        val perPage: UByte? = 100u
        val sortBy: QuerySortBy = QuerySortBy.DateModified
        val sortType: QuerySortType = QuerySortType.Descending
        val tags: MutableSet<Int>? = null
        val date: Pair<Long, Long> = Pair(0L, 0L)
        val dateModified: Pair<Long, Long> = Pair(0L, 0L)
        val title: String = ""
    }
}

class QueryViewModel : ViewModel() {

    private val _page = MutableLiveData(DefaultValues.page)
    val page: LiveData<UShort?> = _page

    private val _perPage = MutableLiveData(DefaultValues.perPage)
    val perPage: LiveData<UByte?> = _perPage

    private val _sortBy = MutableLiveData(DefaultValues.sortBy)
    val sortBy: LiveData<QuerySortBy> = _sortBy

    private val _sortType = MutableLiveData(DefaultValues.sortType)
    val sortType: LiveData<QuerySortType> = _sortType

    private val _tags = MutableLiveData(DefaultValues.tags)
    val tags: LiveData<MutableSet<Int>?> = _tags

    private val _date = MutableLiveData(DefaultValues.date)
    val date: LiveData<Pair<Long, Long>> = _date

    private val _dateModified = MutableLiveData(DefaultValues.dateModified)
    val dateModified: LiveData<Pair<Long, Long>> = _dateModified

    private val _title = MutableLiveData(DefaultValues.title)
    val title: LiveData<String> = _title

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

    fun setTitle(title: String) {
        if (_title.value != title) {
            _title.value = title
        }
    }

    fun setSortBy(sortBy: QuerySortBy) {
        _sortBy.value = sortBy
    }

    fun setSortType(sortType: QuerySortType) {
        _sortType.value = sortType
    }

    fun clearQuery() {
        _page.value = DefaultValues.page
        _perPage.value = DefaultValues.perPage
        _sortBy.value = DefaultValues.sortBy
        _sortType.value = DefaultValues.sortType
        _tags.value = DefaultValues.tags
        _date.value = DefaultValues.date
        _dateModified.value = DefaultValues.dateModified
        _title.value = DefaultValues.title
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
