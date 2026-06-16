package com.mashiverse.data.models

import kotlinx.serialization.Serializable

@Serializable
data class Asset(
    val name: String,
    val image: String
)