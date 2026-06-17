package com.mashiverse.server.routes

import com.mashiverse.data.models.Mashup
import com.mashiverse.data.repos.ImageRepo
import data.models.DownloadType
import data.remote.dto.MashupDto
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.jvm.javaio.toOutputStream
import org.koin.ktor.ext.inject
import java.io.ByteArrayInputStream

fun Application.mashupRoutes() {
    val imageRepo by inject<ImageRepo>()

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

                val image: ByteArray? = imageRepo.getImage(
                    mashup = mashup,
                    downloadType = downloadType,
                    mintedName = mintedName
                )

                if (image == null) {
                    call.respond(HttpStatusCode.InternalServerError)
                    return@post
                }

                val inputStream = ByteArrayInputStream(image)
                call.respondBytesWriter(contentType = mediaType) {
                    inputStream.copyTo(this.toOutputStream())
                }
            } catch (e: Exception) {
                println(e.localizedMessage)
                call.respond(HttpStatusCode.InternalServerError)
            }
        }
    }
}