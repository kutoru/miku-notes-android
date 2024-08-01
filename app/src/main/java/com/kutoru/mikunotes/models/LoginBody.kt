package com.kutoru.mikunotes.models

import kotlinx.serialization.Serializable

@Serializable
data class LoginBody(
    val email: String,
    val password: String,
)
