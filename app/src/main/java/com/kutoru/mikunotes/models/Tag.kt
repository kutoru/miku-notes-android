package com.kutoru.mikunotes.models

import kotlinx.serialization.Serializable

@Serializable
data class Tag(
    val created: Long,
    val id: Int,
    var name: String,
    val note_id: Int?,
    val user_id: Int,
) : java.io.Serializable
