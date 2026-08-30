import java.util.concurrent.TimeUnit

data class ImageAnimationResult(
    val isAnimated: Boolean,
    val frameCount: Int,
    val error: String? = null
)

fun isImageAnimated(bytes: ByteArray, ffprobePath: String = "ffprobe"): ImageAnimationResult {
    if (bytes.isEmpty()) {
        return ImageAnimationResult(isAnimated = false, frameCount = 0, error = "Empty byte array")
    }

    val isPng = isPngHeader(bytes)

    // Attempt 1: For PNGs, force the 'apng' demuxer to discover animation frames
    val primaryResult = executeFfprobe(bytes, ffprobePath, forceApng = isPng)

    // Fallback: If forced APNG demuxing failed on a standard static PNG, retry with generic demuxing
    if (primaryResult.error != null && isPng) {
        return executeFfprobe(bytes, ffprobePath, forceApng = false)
    }

    return primaryResult
}

private fun executeFfprobe(bytes: ByteArray, ffprobePath: String, forceApng: Boolean): ImageAnimationResult {
    val command = mutableListOf(
        ffprobePath,
        "-v", "error",
        "-count_frames",
        "-select_streams", "v:0",
        "-show_entries", "stream=nb_read_frames",
        "-of", "default=nokey=1:noprint_wrappers=1"
    )

    if (forceApng) {
        command.addAll(listOf("-f", "apng"))
    }

    // Read directly from standard input
    command.addAll(listOf("-i", "pipe:0"))

    return try {
        val process = ProcessBuilder(command)
            .redirectErrorStream(false)
            .start()

        // Feed bytes into ffprobe via a background thread to prevent buffer deadlocks
        val writerThread = Thread {
            try {
                process.outputStream.use { it.write(bytes) }
            } catch (_: Exception) {
                // Ignore pipe close if ffprobe terminates early
            }
        }
        writerThread.start()

        var stdout = ""
        var stderr = ""

        val outThread = Thread { stdout = process.inputStream.bufferedReader().readText().trim() }
        val errThread = Thread { stderr = process.errorStream.bufferedReader().readText().trim() }

        outThread.start()
        errThread.start()

        val completed = process.waitFor(5, TimeUnit.SECONDS)
        if (!completed) {
            process.destroyForcibly()
            return ImageAnimationResult(isAnimated = false, frameCount = 0, error = "ffprobe process timed out")
        }

        writerThread.join(500)
        outThread.join(500)
        errThread.join(500)

        if (process.exitValue() != 0) {
            return ImageAnimationResult(
                isAnimated = false,
                frameCount = 0,
                error = stderr.ifBlank { "ffprobe exited with code ${process.exitValue()}" }
            )
        }

        val frameCount = stdout.toIntOrNull() ?: 1
        ImageAnimationResult(
            isAnimated = frameCount > 1,
            frameCount = frameCount,
            error = null
        )
    } catch (e: Exception) {
        ImageAnimationResult(isAnimated = false, frameCount = 0, error = e.message)
    }
}

/**
 * Checks the standard 8-byte PNG header: 89 50 4E 47 0D 0A 1A 0A
 */
private fun isPngHeader(bytes: ByteArray): Boolean {
    if (bytes.size < 8) return false
    val pngSignature = byteArrayOf(
        0x89.toByte(), 0x50.toByte(), 0x4E.toByte(), 0x47.toByte(),
        0x0D.toByte(), 0x0A.toByte(), 0x1A.toByte(), 0x0A.toByte()
    )
    return bytes.take(8).toByteArray().contentEquals(pngSignature)
}