package com.mashiverse.images.playwright.combiners

import com.google.gson.JsonObject
import com.mashiverse.configs.*
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
import org.koin.core.component.inject
import java.nio.file.Paths
import kotlin.math.ceil
import kotlin.math.min

class AnimCombiner : KoinComponent {
    val browser by inject<Browser>()

    fun renderFrameRange(
        context: BrowserContext,
        htmlContent: String,
        startFrame: Int,
        endFrame: Int,
        resourcesDir: String
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
            val framePath = Paths.get(resourcesDir, frameName)

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

    suspend fun generateAnim(tempDir: String, t: Double): String {
        return withContext(Dispatchers.IO) {
            try {
                var maxT = t
                if (maxT < DURATIONS_LIMIT_SEC) {
                    maxT *= ceil(DURATIONS_LIMIT_SEC / maxT)
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
                            val browserCtx = browser.newContext(
                                Browser.NewContextOptions().setViewportSize(ViewportSize(GIF_WIDTH, GIF_HEIGHT))
                            )

                            browserCtx.use { ctx ->
                                renderFrameRange(
                                    context = ctx,
                                    htmlContent = htmlContent,
                                    startFrame = startFrame,
                                    endFrame = endFrame,
                                    resourcesDir = tempDir
                                )
                            }
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

    private fun makeGif(tempDir: String): String {
        val palettePath = Paths.get(tempDir, "palette.png").toString()
        val gifPath = Paths.get(tempDir, "result.gif").toString()

        executeCmd(
            "ffmpeg", "-y", "-threads", "0",
            "-i", "$tempDir/frame_%03d.png",
            "-vf", "format=rgba,palettegen=max_colors=256",
            palettePath
        )

        executeCmd(
            "ffmpeg", "-y", "-threads", "0", "-framerate", "$PLAYBACK_FPS",
            "-i", "$tempDir/frame_%03d.png",
            "-i", palettePath,
            "-lavfi", "[0:v]format=rgba,setpts=PTS-STARTPTS[v];[v][1:v]paletteuse=dither=none",
            gifPath
        )

        return gifPath
    }
}