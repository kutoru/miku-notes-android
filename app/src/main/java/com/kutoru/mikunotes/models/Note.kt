package com.kutoru.mikunotes.models

import kotlinx.serialization.Serializable

@Serializable
data class Note(
    val created: Long,
    val files: MutableList<File>,
    val id: Int,
    val last_edited: Long,
    val tags: MutableList<Tag>,
    var text: String,
    val times_edited: Int,
    var title: String,
    val user_id: Int,
)
