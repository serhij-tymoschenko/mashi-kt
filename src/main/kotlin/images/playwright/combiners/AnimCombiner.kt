package com.mashiverse.images.playwright.combiners

import com.mashiverse.configs.DURATION_LIMIT_SEC
import com.mashiverse.configs.GIF_HEIGHT
import com.mashiverse.configs.GIF_WIDTH
import com.mashiverse.configs.PLAYBACK_FPS
import com.mashiverse.images.playwright.PlaywrightService
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
        return withContext(Dispatchers.IO) {
            try {
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

                PlaywrightService.getBrowser().use { browser ->
                    // 1. Warm-up pass: load images + apply preparePage's padding
                    // correction OFF-CAMERA, then capture the corrected DOM.
                    val warmupCtx = browser.newContext(
                        Browser.NewContextOptions()
                            .setViewportSize(ViewportSize(GIF_WIDTH, GIF_HEIGHT))
                    )
                    val warmupPage = warmupCtx.newPage()
                    warmupPage.setContent(htmlContent)
                    warmupPage.waitForLoadState(LoadState.LOAD)
                    preparePage(warmupPage, getGifArgs()) // mutates padding now, not during recording
                    val correctedHtml = warmupPage.content() // serialize the post-fix DOM
                    warmupCtx.close()

                    // 2. Recording pass: seed with the ALREADY-corrected markup,
                    // so frame 0 is the final layout — no reflow to capture.
                    val browserCtx = browser.newContext(
                        Browser.NewContextOptions()
                            .setViewportSize(ViewportSize(GIF_WIDTH, GIF_HEIGHT))
                            .setRecordVideoDir(tempDir)
                            .setRecordVideoSize(GIF_WIDTH, GIF_HEIGHT)
                    )

                    val page = browserCtx.newPage()

                    // Playwright starts capturing video the instant this page exists,
                    // before any content is painted — that gap shows up as solid-white
                    // lead-in frames in the .webm. Measure exactly how long it takes to
                    // get real pixels on screen, so we can trim precisely that span with
                    // ffmpeg, instead of cutting a fixed/guessed number of frames.
                    val recordingStartedAt = System.nanoTime()
                    page.setContent(correctedHtml)
                    page.waitForFunction(
                        "Array.from(document.images).every(img => img.complete)"
                    )
                    // +1 frame of slack: the JS condition can resolve a tick before the
                    // browser actually paints that state into the recorded track.
                    startOffsetSec = (System.nanoTime() - recordingStartedAt) / 1_000_000_000.0 +
                            (1.0 / PLAYBACK_FPS)

                    val durationMs = (maxT * 1000).toLong()
                    delay(durationMs.milliseconds)

                    browserCtx.close()
                }

                val videoFile = tempDir.toFile().listFiles { _, name -> name.endsWith(".webm") }?.firstOrNull()
                    ?: throw IllegalStateException("Playwright video was not recorded successfully.")

                return@withContext makeGifFromVideo(videoFile.toPath(), tempDir, maxT - startOffsetSec, startOffsetSec)
            } catch (e: Exception) {
                System.err.println("Error in generateAnim: ${e.message}")
                throw e
            }
        }
    }

    private fun makeGifFromVideo(videoPath: Path, tempDir: Path, maxT: Double, startOffsetSec: Double): Path {
        val gifPath = tempDir.resolve("result.gif")
        val seekArg = String.format(java.util.Locale.US, "%.3f", startOffsetSec)
        val durationArg = String.format(java.util.Locale.US, "%.3f", maxT)

        // Common base filters: scale/pad once before splitting to the palette generator
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