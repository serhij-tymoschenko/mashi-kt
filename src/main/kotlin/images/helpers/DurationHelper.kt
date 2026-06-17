package com.mashiverse.images.helpers

import com.mashiverse.configs.ANIM_STEP
import com.mashiverse.data.models.ImageType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.util.Locale
import javax.imageio.ImageIO
import javax.imageio.metadata.IIOMetadataNode


fun getWebpDuration(data: ByteArray): Double {
    return try {
        var totalDurationMs = 0
        ByteArrayInputStream(data).use { bais ->
            ImageIO.createImageInputStream(bais).use { input ->
                val readers = ImageIO.getImageReadersByFormatName("webp")
                if (!readers.hasNext()) return 0.0

                val reader = readers.next()
                reader.input = input
                val numImages = reader.getNumImages(true)

                // TwelveMonkeys reads frames as an image sequence
                for (i in 0 until numImages) {
                    val metadata = reader.getImageMetadata(i)
                    val root = metadata.getAsTree(metadata.nativeMetadataFormatName) as IIOMetadataNode

                    // WebP animation frames are held in "VP8X", "ANIM", or "ANMF" chunks
                    val frameNodes = root.getElementsByTagName("Frame")
                    if (frameNodes.length > 0) {
                        val frameNode = frameNodes.item(0) as IIOMetadataNode
                        val duration = frameNode.getAttribute("duration").toIntOrNull() ?: 0
                        totalDurationMs += duration
                    }
                }
                reader.dispose()
            }
        }
        totalDurationMs / 1000.0
    } catch (e: Exception) {
        0.0
    }
}

fun getGifDuration(data: ByteArray): Double {
    return try {
        var totalDurationMs = 0
        ByteArrayInputStream(data).use { bais ->
            ImageIO.createImageInputStream(bais).use { input ->
                val readers = ImageIO.getImageReadersByFormatName("gif")
                if (!readers.hasNext()) return 0.0

                val reader = readers.next()
                reader.input = input
                val numImages = reader.getNumImages(true)

                for (i in 0 until numImages) {
                    val metadata = reader.getImageMetadata(i)
                    val tree = metadata.getAsTree("javax_imageio_gif_image_1.0") as IIOMetadataNode
                    val gceNodes = tree.getElementsByTagName("GraphicControlExtension")

                    if (gceNodes.length > 0) {
                        val gce = gceNodes.item(0) as IIOMetadataNode
                        val delay = gce.getAttribute("delayTime").toIntOrNull() ?: 0
                        // delayTime is specified in hundredths of a second (10ms)
                        totalDurationMs += delay * 10
                    }
                }
                reader.dispose()
            }
        }
        totalDurationMs / 1000.0
    } catch (e: Exception) {
        0.0
    }
}

fun getApngDuration(data: ByteArray): Double {
    try {
        var totalDuration = 0.0
        var index = 8 // Skip standard PNG 8-byte signature

        while (index + 8 <= data.size) {
            // Read 4-byte big-endian chunk length
            val length =
                ((data[index].toInt() and 0xFF) shl 24) or ((data[index + 1].toInt() and 0xFF) shl 16) or ((data[index + 2].toInt() and 0xFF) shl 8) or (data[index + 3].toInt() and 0xFF)

            val chunkType = String(data, index + 4, 4, Charsets.US_ASCII)

            // fcTL (Frame Control Chunk) contains delay information
            if (chunkType == "fcTL" && index + 34 <= data.size) {
                // Read delay_num (offset 20 from chunk data start) -> index + 8 + 20
                val delayNum = ((data[index + 28].toInt() and 0xFF) shl 8) or (data[index + 29].toInt() and 0xFF)
                // Read delay_den (offset 22 from chunk data start) -> index + 8 + 22
                val delayDen = ((data[index + 30].toInt() and 0xFF) shl 8) or (data[index + 31].toInt() and 0xFF)

                val denom = if (delayDen != 0) delayDen.toDouble() else 100.0
                totalDuration += delayNum.toDouble() / denom
            }

            // Advance pointer: 4 bytes length + 4 bytes type + payload length + 4 bytes CRC
            index += 12 + length
        }
        return totalDuration
    } catch (e: Exception) {
        return 0.0
    }
}

private fun getDuration(data: ByteArray): Double {
    val imageType = getImageType(data)

    return when (imageType) {
        ImageType.GIF -> getGifDuration(data)
        ImageType.WEBP -> getWebpDuration(data)
        ImageType.APNG -> getApngDuration(data)
        else -> ANIM_STEP
    }
}

suspend fun getMaxDuration(traits: List<ByteArray>): Double {
    val max = withContext(Dispatchers.IO) {
        val jobs = traits.map { trait ->
            async { getDuration(trait) }
        }

        val durations = jobs.awaitAll()
        // Handle potential empty list to avoid NoSuchElementException
        durations.maxOrNull() ?: 0.0
    }

    // Formats to 2 decimal places and converts back to Double
    return String.format(Locale.US, "%.2f", max).toDouble()
}