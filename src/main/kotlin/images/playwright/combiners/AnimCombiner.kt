package com.mashiverse.images.playwright.combiners

import com.google.gson.JsonObject
import com.mashiverse.configs.*
import com.mashiverse.images.playwright.PlaywrightService
import com.mashiverse.utils.helpers.executeCmd
import com.mashiverse.utils.helpers.readImageFiles
import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserContext
import com.microsoft.playwright.Page
import com.microsoft.playwright.options.ScreenshotType
import com.microsoft.playwright.options.ViewportSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.math.ceil
import kotlin.math.min

class AnimCombiner : KoinComponent {
    fun renderFrameRange(
        context: BrowserContext,
        htmlContent: String,
        startFrame: Int,
        endFrame: Int,
        resourcesDir: Path
    ) {
        val page = context.newPage()

        page.setContent(htmlContent)
        preparePage(page, getGifArgs())

        val client = page.context().newCDPSession(page)

        val initialPolicyArgs = JsonObject().apply {
            addProperty("policy", "advance")
            addProperty("budget", startFrame * FRAME_DELAY_MS)
        }

        client.send(
            "Emulation.setVirtualTimePolicy",
            initialPolicyArgs
        )

        for (i in startFrame..endFrame) {
            val frameName = String.format("frame_%03d.png", i)
            val framePath = resourcesDir.resolve(frameName)

            page.screenshot(
                Page.ScreenshotOptions()
                    .setPath(framePath)
                    .setType(ScreenshotType.PNG)
                    .setOmitBackground(false)
            )

            val policyArgs = JsonObject().apply {
                addProperty("policy", "advance")
                addProperty("budget", FRAME_DELAY_MS)
            }

            client.send(
                "Emulation.setVirtualTimePolicy",
                policyArgs
            )
        }

        page.close()
    }

    suspend fun generateAnim(tempDir: Path, t: Double): Path {
        return withContext(Dispatchers.IO) {
            try {
                var maxT = t
                print(maxT)
                if (maxT < DURATION_LIMIT_SEC * 2) {
                    maxT *= ceil(DURATION_LIMIT_SEC * 2 / maxT)
                }

                val totalFrames = ceil(maxT * CAPTURE_FPS).toInt()
                val imageUrls = readImageFiles(tempDir)

                val htmlContent = prepareHtml(
                    urls = imageUrls,
                    width = GIF_WIDTH,
                    height = GIF_HEIGHT
                )

                val totalJobs = 4
                val chunkSize = (totalFrames + totalJobs - 1) / totalJobs

                val jobs = (0 until totalJobs).mapNotNull { i ->
                    val startFrame = i * chunkSize
                    val endFrame = min((i + 1) * chunkSize - 1, totalFrames)

                    if (startFrame <= endFrame) {
                        async {
                            val browser = PlaywrightService.getBrowser()
                            browser.use { browser ->
                                val browserCtx = browser.newContext(
                                    Browser.NewContextOptions().setViewportSize(ViewportSize(GIF_WIDTH, GIF_HEIGHT))
                                )

                                renderFrameRange(
                                    context = browserCtx,
                                    htmlContent = htmlContent,
                                    startFrame = startFrame,
                                    endFrame = endFrame,
                                    resourcesDir = tempDir
                                )
                            }

                            browser.close()
                        }
                    } else {
                        null
                    }
                }

                jobs.awaitAll()

                return@withContext makeGif(tempDir)
            } catch (e: Exception) {
                System.err.println("Error in generateAnim: ${e.message}")
                throw e
            }
        }
    }

    private fun makeGif(tempDir: Path): Path {
        val palettePath = tempDir.resolve("palette.png")
        val gifPath = tempDir.resolve("result.gif")
        val ffmpegInputPattern = tempDir.resolve("frame_%03d.png").absolutePathString()

        executeCmd(
            "ffmpeg", "-y", "-threads", "0",
            "-i", ffmpegInputPattern,
            "-vf", "format=rgba,palettegen=max_colors=256",
            palettePath.absolutePathString()
        )

        executeCmd(
            "ffmpeg", "-y", "-threads", "0", "-framerate", "$PLAYBACK_FPS",
            "-i", ffmpegInputPattern,
            "-i", palettePath.absolutePathString(),
            "-lavfi", "[0:v]format=rgba,setpts=PTS-STARTPTS[v];[v][1:v]paletteuse=dither=none",
            gifPath.absolutePathString()
        )

        return gifPath
    }
}