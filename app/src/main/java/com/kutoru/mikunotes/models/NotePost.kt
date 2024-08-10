package com.kutoru.mikunotes.models

import kotlinx.serialization.Serializable

@Serializable
data class NotePost(
    val text: String,
    val title: String,
)
