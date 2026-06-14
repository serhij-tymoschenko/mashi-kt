package com.mashiverse.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object KtorClient {
    val jsonDecoder = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        isLenient = true
    }

    fun client() = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(jsonDecoder)
        }
    }
}