package com.mashiverse.utils

import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readBytes
import kotlin.io.path.writeBytes

fun executeCmd(vararg command: String) {
    try {
        val process = ProcessBuilder(*command)
            .inheritIO()
            .start()
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            throw IOException("Command failed with exit code $exitCode: ${command.joinToString(" ")}")
        }
    } catch (e: Exception) {
        throw RuntimeException("Failed to run system command", e)
    }
}

fun readImageFiles(folderPath: String): List<String> {
    val dir = File(folderPath)

    if (!dir.exists() || !dir.isDirectory) {
        System.err.println("Error: Provided path is not a valid directory.")
        return emptyList()
    }

    try {
        val numericFiles = dir.listFiles()
            ?.filter { file -> file.isFile && file.name.matches(Regex("^\\d+\$")) }
            ?.sortedBy { file -> file.name.toInt() }
            ?: return emptyList()

        return numericFiles.map { file ->
            // Using Kotlin's built-in File extension function instead of Files.readAllBytes
            val bytes = file.readBytes()
            String(bytes, StandardCharsets.UTF_8)
        }
    } catch (e: Exception) {
        System.err.println("Error reading files: ${e.message}")
        return emptyList()
    }
}

fun readFile(filePath: Path): ByteArray {
    return filePath.readBytes()
}

fun saveFile(filePath: Path, content: ByteArray) {
    filePath.writeBytes(content)
}

fun rmDir(dirPath: Path) {
    if (dirPath.exists()) {
        dirPath.toFile().walkBottomUp().forEach { file ->
            file.delete()
        }
    }
}