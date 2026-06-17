package com.mashiverse.server.routes

import com.mashiverse.data.db.daos.ImageDao
import com.mashiverse.data.models.ImageType
import com.mashiverse.data.remote.apis.IpfsApi
import com.mashiverse.images.converters.SvgProcessor
import com.mashiverse.images.converters.convertApngToWebp
import com.mashiverse.images.helpers.getImageType
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Application.correctionRoutes() {
    val imageDao by inject<ImageDao>()
    val ipfsApi by inject<IpfsApi>()

    routing {
        get("/api/apng/{image_id}") {
            val imageId = call.parameters["image_id"]
                ?: return@get call.respond(HttpStatusCode.BadRequest)

            val url = "https://ipfs.io/ipfs/$imageId"

            try {
                var src: ByteArray? = imageDao.getWebpImage(url)

                if (src == null) {
                    val apngBytes = ipfsApi.getImageSrc(url)

                    src = convertApngToWebp(apngBytes!!)

                    if (getImageType(src) == ImageType.WEBP) {
                        imageDao.addWebpImage(url, src)
                    }
                }

                call.respondBytes(src, ContentType.Image.WEBP)

            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError)
            }
        }

        // Equivalent to @correction_router.get("/api/svg/{image_id}")
        get("/api/svg/{image_id}") {
            val imageId = call.parameters["image_id"]
                ?: return@get call.respond(HttpStatusCode.BadRequest)

            val url = "https://ipfs.io/ipfs/$imageId"

            try {
                // Check cache
                var src: ByteArray? = imageDao.getSvgImage(url)

                if (src == null) {
                    val svgBytes = ipfsApi.getImageSrc(url)

                    src = SvgProcessor.processSvg(svgBytes!!)

                    if (getImageType(src) == ImageType.SVG) {
                        imageDao.addSvgImage(url, src)
                    }
                }

                call.respondBytes(src, ContentType.parse("image/svg+xml"))

            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError)
            }
        }
    }
}