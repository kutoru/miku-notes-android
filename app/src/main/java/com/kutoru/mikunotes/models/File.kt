package com.kutoru.mikunotes.models

import kotlinx.serialization.Serializable

@Serializable
data class File(
    var attach_id: Int?,
    val created: Long,
    val hash: String,
    val id: Int,
    val name: String,
    val size: Long,
    val user_id: Int,
) : java.io.Serializable
