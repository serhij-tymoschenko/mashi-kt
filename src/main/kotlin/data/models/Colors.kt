package com.mashiverse.data.models

import kotlinx.serialization.Serializable

@Serializable
data class Colors(
    val base: String,
    val eyes: String,
    val hair: String
)