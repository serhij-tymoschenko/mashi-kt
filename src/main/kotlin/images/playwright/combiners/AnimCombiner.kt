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
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import java.nio.file.Path
import kotlin.io.path.absolutePathString

class AnimCombiner : KoinComponent {

    suspend fun generateAnim(tempDir: Path, t: Double): Path {
        // Target duration fixed to exactly 5 seconds
        val targetDurationSec = DURATION_LIMIT_SEC.toDouble()

        val imageUrls = readImageFiles(tempDir)
        val htmlContent = prepareHtml(
            urls = imageUrls,
            width = GIF_WIDTH,
            height = GIF_HEIGHT
        )

        var startOffsetSec = 0.0

        PlaywrightPool.execute { browser ->
            // 1. Warm-up pass
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

            // 2. Recording pass
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

                // Time elapsed from when the recording started until the page was ready
                startOffsetSec = (System.nanoTime() - recordingStartedAt) / 1_000_000_000.0 + (1.0 / PLAYBACK_FPS)

                // Sleep for: target duration (5.0s) + initial loading offset + 0.5s safety buffer
                // This ensures FFmpeg has more than enough frames to cut the full 5.0s cleanly
                val totalSleepMs = ((targetDurationSec + startOffsetSec + 0.5) * 1000).toLong()
                Thread.sleep(totalSleepMs)

                // Flush encoder buffers
                page.close()
            }
        }

        val videoFile = withContext(Dispatchers.IO) {
            tempDir.toFile().listFiles { _, name -> name.endsWith(".webm") }?.firstOrNull()
                ?: throw IllegalStateException("Playwright video was not recorded successfully.")
        }

        return withContext(Dispatchers.IO) {
            makeGifFromVideo(
                videoPath = videoFile.toPath(),
                tempDir = tempDir,
                durationSec = targetDurationSec,
                startOffsetSec = startOffsetSec
            )
        }
    }

    private fun makeGifFromVideo(
        videoPath: Path,
        tempDir: Path,
        durationSec: Double,
        startOffsetSec: Double
    ): Path {
        val gifPath = tempDir.resolve("result.gif")

        // Format timestamps strictly
        val seekArg = String.format(java.util.Locale.US, "%.3f", startOffsetSec)
        val durationArg = String.format(java.util.Locale.US, "%.3f", durationSec) // "5.000"

        val baseFilter = "fps=$PLAYBACK_FPS,scale=$GIF_WIDTH:$GIF_HEIGHT:force_original_aspect_ratio=decrease,pad=$GIF_WIDTH:$GIF_HEIGHT:(ow-iw)/2:(oh-ih)/2,setsar=1"

        // diff_mode=none prevents loop artifacting; bayer dithering keeps size down with 256 colors
        val filterGraph = "[0:v]$baseFilter,split[stream][paletteSource];" +
                "[paletteSource]palettegen=max_colors=256:stats_mode=diff[palette];" +
                "[stream][palette]paletteuse=dither=bayer:bayer_scale=3:diff_mode=none"

        // Step 1: Extract exactly 5 seconds
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

        // Step 2: Lossy compression keeping exactly 5 seconds and 256 colors
        executeCmd(
            "gifsicle",
            "-b",
            "-O3",
            "--lossy=80",
            "--colors", "256",
            "--careful",
            "--no-comments",
            "--no-names",
            "--no-extensions",
            gifPath.absolutePathString()
        )

        return gifPath
    }
}