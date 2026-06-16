package com.mashiverse.images.helpers

import com.mashiverse.data.models.ImageType
import com.mashiverse.utils.helpers.indexOfSequence

fun getMime(data: ByteArray): String {
    return try {
        val imageType = getImageType(data)

        when (imageType) {
            ImageType.SVG -> "image/svg+xml"
            ImageType.WEBP -> "image/webp"
            ImageType.GIF -> "image/gif"
            else -> "image/png"
        }
    } catch (e: Exception) {
        println(e)
        "image/png"
    }
}

fun getImageType(data: ByteArray): ImageType {
    return try {
        // Trim leading whitespace bytes
        val leadingIdx =
            data.indexOfFirst { it != ' '.code.toByte() && it != '\n'.code.toByte() && it != '\r'.code.toByte() }
        if (leadingIdx != -1 && leadingIdx + 4 <= data.size) {
            val prefix = String(data.sliceArray(leadingIdx until leadingIdx + 4), Charsets.UTF_8)
            if (prefix.startsWith("<svg")) return ImageType.SVG
        }

        val size = data.size
        if (size < 4) return ImageType.UNKNOWN

        // Convert first 4 bytes to Hex string for easy matching
        val hexMarker = data.take(4).joinToString("") { "%02X".format(it) }

        when {
            hexMarker.startsWith("47494638") -> ImageType.GIF // GIF87a or GIF89a
            hexMarker == "52494646" && size >= 12 && String(data.sliceArray(8..11)) == "WEBP" -> ImageType.WEBP
            data.indexOfSequence("acTL".toByteArray()) != -1 -> ImageType.APNG
            hexMarker == "89504E47" -> ImageType.PNG
            else -> ImageType.UNKNOWN
        }
    } catch (e: Exception) {
        println(e)
        ImageType.UNKNOWN
    }
}