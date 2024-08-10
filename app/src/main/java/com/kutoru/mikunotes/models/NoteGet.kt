package com.kutoru.mikunotes.models

import kotlinx.serialization.Serializable

@Serializable
data class NoteGet(
    val notes: MutableList<Note>,
)
