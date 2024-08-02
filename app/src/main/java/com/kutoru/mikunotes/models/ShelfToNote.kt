package com.kutoru.mikunotes.models

import kotlinx.serialization.Serializable

@Serializable
data class ShelfToNote(
    val note_title: String,
    val note_text: String,
)
