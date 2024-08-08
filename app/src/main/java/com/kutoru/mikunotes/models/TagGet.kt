package com.kutoru.mikunotes.models

import kotlinx.serialization.Serializable

@Serializable
data class TagGet(
    val tags: MutableList<Tag>,
)
