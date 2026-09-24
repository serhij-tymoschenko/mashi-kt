data class ImageAnimationResult(
    val isAnimated: Boolean,
    val format: String = "unknown"
)

fun isImageAnimated(bytes: ByteArray): ImageAnimationResult {
    if (bytes.size < 12) return ImageAnimationResult(isAnimated = false)

    // 1. Check GIF89a (GIF87a is standard static)
    if (isGif89a(bytes)) {
        return ImageAnimationResult(isAnimated = true, format = "GIF")
    }

    // 2. Check APNG (Looks for "acTL" animation control chunk in PNG structure)
    if (isApng(bytes)) {
        return ImageAnimationResult(isAnimated = true, format = "APNG")
    }

    // 3. Check WebP (Looks for RIFF header and "ANIM" chunk)
    if (isAnimatedWebP(bytes)) {
        return ImageAnimationResult(isAnimated = true, format = "WebP")
    }

    return ImageAnimationResult(isAnimated = false)
}

/**
Checks for GIF89a header (47 49 46 38 39 61)
 */
private fun isGif89a(bytes: ByteArray): Boolean {
    return bytes.size >= 6 &&
            bytes[0] == 0x47.toByte() && // G
            bytes[1] == 0x49.toByte() && // I
            bytes[2] == 0x46.toByte() && // F
            bytes[3] == 0x38.toByte() && // 8
            bytes[4] == 0x39.toByte() && // 9
            bytes[5] == 0x61.toByte()    // a
}

/**
Scans PNG chunks for the 'acTL' (Animation Control) chunk
 */
private fun isApng(bytes: ByteArray): Boolean {
    // Check standard 8-byte PNG signature
    val pngHeader = byteArrayOf(
        0x89.toByte(), 0x50.toByte(), 0x4E.toByte(), 0x47.toByte(),
        0x0D.toByte(), 0x0A.toByte(), 0x1A.toByte(), 0x0A.toByte()
    )
    if (bytes.size < 8 || !bytes.sliceArray(0..7).contentEquals(pngHeader)) {
        return false
    }

    // Search for "acTL" (0x61, 0x63, 0x54, 0x4C) chunk across first 4KB
    val searchLimit = minOf(bytes.size - 4, 4096)
    for (i in 8 until searchLimit) {
        if (bytes[i] == 0x61.toByte() &&
            bytes[i + 1] == 0x63.toByte() &&
            bytes[i + 2] == 0x54.toByte() &&
            bytes[i + 3] == 0x4C.toByte()
        ) {
            return true
        }
    }
    return false
}

/**
Scans WebP RIFF container for "ANIM" chunk
 */
private fun isAnimatedWebP(bytes: ByteArray): Boolean {
    if (bytes.size < 12) return false

    // Check 'RIFF' and 'WEBP'
    val isRiff = bytes[0] == 'R'.code.toByte() && bytes[1] == 'I'.code.toByte() &&
            bytes[2] == 'F'.code.toByte() && bytes[3] == 'F'.code.toByte()
    val isWebp = bytes[8] == 'W'.code.toByte() && bytes[9] == 'E'.code.toByte() &&
            bytes[10] == 'B'.code.toByte() && bytes[11] == 'P'.code.toByte()

    if (!isRiff || !isWebp) return false

    // Search for "ANIM" chunk across first 2KB
    val searchLimit = minOf(bytes.size - 4, 2048)
    for (i in 12 until searchLimit) {
        if (bytes[i] == 'A'.code.toByte() &&
            bytes[i + 1] == 'N'.code.toByte() &&
            bytes[i + 2] == 'I'.code.toByte() &&
            bytes[i + 3] == 'M'.code.toByte()
        ) {
            return true
        }
    }
    return false
}