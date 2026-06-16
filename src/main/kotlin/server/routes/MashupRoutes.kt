package com.mashiverse.server.routes

import com.mashiverse.data.models.Mashup
import data.models.DownloadType
import data.remote.dto.MashupDto
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing

fun Application.mashupRoutes() {
    routing {
        post("/api/mashi/app_mashup") {
            try {
                val downloadTypeParam = call.request.queryParameters["download_type"] ?: "png"
                val mintedName = call.request.queryParameters["minted_name"]

                val data = call.receive<MashupDto>()

                val mashup = Mashup(
                    colors = data.colors,
                    traits = data.assets
                )

                val mediaType = when (downloadTypeParam.lowercase()) {
                    "png" -> ContentType.Image.PNG
                    "gif" -> ContentType.Image.GIF
                    else -> {
                        call.respond(HttpStatusCode.BadRequest)
                        return@post
                    }
                }

                val downloadType = DownloadType.valueOf(downloadTypeParam.uppercase())

                val image: ByteArray? =
            }
        }
    }
}