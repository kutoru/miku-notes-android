package com.kutoru.mikunotes.models

data class NoteQueryParameters(
    val page: UShort?,
    val per_page: UByte?,
    val sort_by: QuerySortBy?,
    val sort_type: QuerySortType?,
    val tags: Set<Int>?,
    val date: Pair<Long, Long>?,
    val date_modif: Pair<Long, Long>?,
    val title: String?,
)

enum class QuerySortBy {
    Date, DateModified, Title;

    override fun toString(): String {
        return when(this) {
            Date -> "date"
            DateModified -> "date_modif"
            Title -> "title"
        }
    }
}

enum class QuerySortType {
    Ascending, Descending;

    override fun toString(): String {
        return when(this) {
            Ascending -> "asc"
            Descending -> "desc"
        }
    }
}
