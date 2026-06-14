package com.mashiverse.data.remote.apis

import com.mashiverse.configs.MASHIT_BASE_URL
import com.mashiverse.configs.MASHIT_API_KEY
import com.mashiverse.data.remote.KtorClient
import com.mashiverse.data.remote.dto.ListingDto
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
    val client by inject<HttpClient>()

    suspend fun getShopItem(itemId: String, apiKey: String = MASHIT_API_KEY): ListingDto {
        val response = client.get("$MASHIT_BASE_URL/api/v1/listings/$itemId") {
            parameter("apiKey", apiKey)
        }

        if (!response.status.isSuccess()) {
            throw IllegalStateException("HTTP Error: ${response.status} while fetching item $itemId")
        }

        // Ktor seamlessly deserializes JSON directly into your type-safe DTO class
        return response.body()
    }

    suspend fun getMashup(wallet: String): MashupDto? {
        return try {
            // Fix potential missing trailing slash in config cleanly
            val sanitizedBase = if (MASHIT_BASE_URL.endsWith("/")) MASHIT_BASE_URL else "$MASHIT_BASE_URL/"

            val response: HttpResponse = client.get("${sanitizedBase}api/mashers/latest") {
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