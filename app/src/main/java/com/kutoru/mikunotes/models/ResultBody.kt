package com.kutoru.mikunotes.models

import kotlinx.serialization.Serializable

@Serializable
data class ResultBody<T>(
    val data: T?,
    val error: String?,
    val success: Boolean,
)
