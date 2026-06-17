package com.mashiverse.data.remote.apis

import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Duration.Companion.milliseconds


class IpfsApi : KoinComponent {
    private val client by inject<HttpClient>()

    suspend fun getImageSrc(imageUrl: String, maxRetries: Int = 5): ByteArray? {
        val url = imageUrl.replace("ipfs.", "ipfs.filebase.")

        for (attempt in 0 until maxRetries) {
            try {
                // Execute a plain GET request expecting raw binary data back
                val response = client.get(url) {
                    // Ktor equivalents to setting timeouts can be done via HttpTimeout plugin if needed globally,
                    // but plain requests default to standard CIO connection thresholds.
                }

                if (response.status == HttpStatusCode.OK) {
                    return response.bodyAsBytes()
                }

                if (response.status == HttpStatusCode.NotFound) {
                    break // Fall through to next URL base immediately on 404
                }

            } catch (e: Exception) {
                println("Attempt ${attempt + 1} failed for $url: ${e.localizedMessage}")
            }

            // Pure non-blocking coroutine suspension delay (Replaces time.sleep)
            delay(1000.milliseconds)
        }

        return null
    }
}