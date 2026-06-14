package com.mashiverse.data.db.entities

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: Long,
    val wallet: String,
    val reactionCount: Int = 0
)
