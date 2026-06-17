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
        // Construct the list of URLs to fallback through, mirroring your Python array
        val urlsToTry = listOf(
            imageUrl,
            imageUrl.replace("ipfs.", "ipfs.filebase.")
        )

        for (url in urlsToTry) {
            for (attempt in 0 until maxRetries) {
                try {
                    val response = client.get(url)

                    if (response.status == HttpStatusCode.OK) {
                        return response.bodyAsBytes()
                    }

                    if (response.status == HttpStatusCode.NotFound) {
                        // Break out of the inner retry loop to try the next URL immediately
                        break
                    }

                } catch (e: Exception) {
                    println("Attempt ${attempt + 1} failed for $url: ${e.localizedMessage}")
                }

                // Non-blocking coroutine delay before the next retry attempt
                delay(1000.milliseconds)
            }
        }

        return null
    }
}