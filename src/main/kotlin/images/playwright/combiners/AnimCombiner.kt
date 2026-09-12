package com.mashiverse.images.playwright.combiners

import com.mashiverse.configs.DURATION_LIMIT_SEC
import com.mashiverse.configs.GIF_HEIGHT
import com.mashiverse.configs.GIF_WIDTH
import com.mashiverse.configs.PLAYBACK_FPS
import com.mashiverse.images.playwright.PlaywrightPool
import com.mashiverse.utils.helpers.executeCmd
import com.mashiverse.utils.helpers.readImageFiles
import com.microsoft.playwright.Browser
import com.microsoft.playwright.options.LoadState
import com.microsoft.playwright.options.ViewportSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.math.ceil
import kotlin.time.Duration.Companion.milliseconds

class AnimCombiner : KoinComponent {

    suspend fun generateAnim(tempDir: Path, t: Double): Path {
        var maxT = t
        if (maxT < DURATION_LIMIT_SEC) {
            maxT *= ceil(DURATION_LIMIT_SEC / maxT)
        }

        val imageUrls = readImageFiles(tempDir)
        val htmlContent = prepareHtml(
            urls = imageUrls,
            width = GIF_WIDTH,
            height = GIF_HEIGHT
        )

        var startOffsetSec = 0.0

        PlaywrightPool.execute { browser ->
            // 1. Warm-up pass: load images + apply padding correction off-camera
            val warmupCtx = browser.newContext(
                Browser.NewContextOptions().setViewportSize(ViewportSize(GIF_WIDTH, GIF_HEIGHT))
            )
            val correctedHtml = warmupCtx.use { ctx ->
                val warmupPage = ctx.newPage()
                warmupPage.setContent(htmlContent)
                warmupPage.waitForLoadState(LoadState.LOAD)
                preparePage(warmupPage, getGifArgs())
                warmupPage.content()
            }

            // 2. Recording pass: record against the pre-corrected DOM markup
            val recordingCtx = browser.newContext(
                Browser.NewContextOptions()
                    .setViewportSize(ViewportSize(GIF_WIDTH, GIF_HEIGHT))
                    .setRecordVideoDir(tempDir)
                    .setRecordVideoSize(GIF_WIDTH, GIF_HEIGHT)
            )

            recordingCtx.use { ctx ->
                val page = ctx.newPage()
                val recordingStartedAt = System.nanoTime()
                page.setContent(correctedHtml)
                page.waitForFunction("Array.from(document.images).every(img => img.complete)")

                startOffsetSec = (System.nanoTime() - recordingStartedAt) / 1_000_000_000.0 + (1.0 / PLAYBACK_FPS)

                val durationMs = (maxT * 1000).toLong()
                Thread.sleep(durationMs) // Keep worker thread blocked for recording duration
            }
        }

        val videoFile = withContext(Dispatchers.IO) {
            tempDir.toFile().listFiles { _, name -> name.endsWith(".webm") }?.firstOrNull()
                ?: throw IllegalStateException("Playwright video was not recorded successfully.")
        }

        return withContext(Dispatchers.IO) {
            makeGifFromVideo(videoFile.toPath(), tempDir, maxT - startOffsetSec, startOffsetSec)
        }
    }

    private fun makeGifFromVideo(videoPath: Path, tempDir: Path, maxT: Double, startOffsetSec: Double): Path {
        val gifPath = tempDir.resolve("result.gif")
        val seekArg = String.format(java.util.Locale.US, "%.3f", startOffsetSec)
        val durationArg = String.format(java.util.Locale.US, "%.3f", maxT)

        val baseFilter = "fps=$PLAYBACK_FPS,scale=$GIF_WIDTH:$GIF_HEIGHT:force_original_aspect_ratio=decrease,pad=$GIF_WIDTH:$GIF_HEIGHT:(ow-iw)/2:(oh-ih)/2,setsar=1"
        val filterGraph = "[0:v]$baseFilter,split[stream][paletteSource];[paletteSource]palettegen=max_colors=256[palette];[stream][palette]paletteuse=dither=none"

        executeCmd(
            "ffmpeg",
            "-y",
            "-threads", "0",
            "-ss", seekArg,
            "-t", durationArg,
            "-i", videoPath.absolutePathString(),
            "-filter_complex", filterGraph,
            gifPath.absolutePathString()
        )

        return gifPath
    }
}