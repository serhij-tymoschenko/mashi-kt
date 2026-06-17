package com.mashiverse.data.remote.apis

import com.mashiverse.configs.MASHIT_BASE_URL
import com.mashiverse.configs.MASHIT_API_KEY
import com.mashiverse.data.remote.KtorClient
import data.remote.dto.MashupDto
import io.ktor.client.HttpClient

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.getValue

class MashitApi : KoinComponent {
    private val client by inject<HttpClient>()

    suspend fun getMashup(wallet: String): MashupDto? {
        return try {
            val response: HttpResponse = client.get("$MASHIT_BASE_URL/api/mashers/latest") {
                parameter("wallet", wallet)
            }

            // Read as raw text first if you need to inspect conditional string messages
            val responseText = response.bodyAsText()

            if (responseText.contains("No mashups found")) {
                return null
            }

            // Explicitly parse the verified JSON string into the DTO
            KtorClient.jsonDecoder.decodeFromString<MashupDto>(responseText)

        } catch (e: Exception) {
            println("Error fetching mashup for wallet $wallet: ${e.localizedMessage}")
            null
        }
    }
}