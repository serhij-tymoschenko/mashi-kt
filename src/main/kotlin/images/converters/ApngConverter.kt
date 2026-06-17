package com.mashiverse.images.converters

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

suspend fun convertApngToWebp(apngBytes: ByteArray): ByteArray = withContext(Dispatchers.IO) {
    val tempInputFile = File.createTempFile("animated_input_", ".apng")
    val tempOutputFile = File.createTempFile("animated_output_", ".webp")

    try {
        tempInputFile.writeBytes(apngBytes)

        val processBuilder = ProcessBuilder(
            "ffmpeg",
            "-y",
            "-i", tempInputFile.absolutePath,
            "-c:v", "libwebp_anim",
            "-lossless", "1",
            "-loop", "0",
            tempOutputFile.absolutePath
        )

        val process = processBuilder.start()

        // Drain stderr concurrently to avoid deadlock on large logs
        val errorOutput = StringBuilder()
        val errorThread = Thread {
            process.errorStream.bufferedReader().forEachLine { errorOutput.appendLine(it) }
        }
        errorThread.start()

        val exitCode = process.waitFor()
        errorThread.join()

        if (exitCode != 0) {
            throw RuntimeException("FFmpeg failed with exit code $exitCode: $errorOutput")
        }

        return@withContext tempOutputFile.readBytes()

    } finally {
        try {
            tempInputFile.delete()
        } catch (_: Exception) {
        }
        try {
            tempOutputFile.delete()
        } catch (_: Exception) {
        }
    }
}