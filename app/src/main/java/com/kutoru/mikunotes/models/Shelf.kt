package com.kutoru.mikunotes.models

import kotlinx.serialization.Serializable

@Serializable
data class Shelf(
    val id: Int,
    val files: Array<File>,
    val created: Long,
    val last_edited: Long,
    val text: String,
    val times_edited: Int,
    val user_id: Int,
)
