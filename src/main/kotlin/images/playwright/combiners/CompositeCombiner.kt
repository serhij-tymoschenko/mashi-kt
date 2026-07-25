package com.mashiverse.images.playwright.combiners

import com.google.gson.JsonObject
import com.mashiverse.configs.PNG_HEIGHT
import com.mashiverse.configs.PNG_WIDTH
import com.mashiverse.images.playwright.PlaywrightService
import com.mashiverse.utils.helpers.executeCmd
import com.mashiverse.utils.helpers.readImageFiles
import com.microsoft.playwright.Browser
import com.microsoft.playwright.Page
import com.microsoft.playwright.options.LoadState
import com.microsoft.playwright.options.ScreenshotType
import com.microsoft.playwright.options.ViewportSize
import org.koin.core.component.KoinComponent
import java.nio.file.Path
import kotlin.io.path.absolutePathString

class CompositeCombiner : KoinComponent {
    fun generateComposite(
        tempDir: Path,
        greyscale: Boolean = false,
        pixelate: Boolean = false
    ): Path {
        val browser = PlaywrightService.getBrowser()
        val frameName = String.format("frame_%03d.png", 0)
        val framePath = tempDir.resolve(frameName)

        try {
            val imageUrls = readImageFiles(tempDir)
            val htmlContent = prepareHtml(
                urls = imageUrls,
                width = PNG_WIDTH,
                height = PNG_HEIGHT,
                greyscale = greyscale
            )

            // Using .use guarantees clean resource teardown automatically
            browser.use { b ->
                val context = b.newContext(
                    Browser.NewContextOptions().setViewportSize(ViewportSize(PNG_WIDTH, PNG_HEIGHT))
                )

                context.use { ctx ->
                    val page = ctx.newPage()
                    page.setContent(htmlContent)
                    preparePage(page, getPngArgs())


                    // CRITICAL: Force Playwright to wait until all assets/images are fully loaded
                    page.waitForLoadState(LoadState.NETWORKIDLE)
                    page.waitForFunction(
                        "Array.from(document.images).every(img => img.complete)"
                    )

                    val client = page.context().newCDPSession(page)

                    val initialPolicyArgs = JsonObject().apply {
                        addProperty("policy", "advance")
                        addProperty("budget", 0)
                    }

                    client.send(
                        "Emulation.setVirtualTimePolicy",
                        initialPolicyArgs
                    )

                    // Capture immediately now that rendering and time budget are synchronized
                    page.screenshot(
                        Page.ScreenshotOptions()
                            .setPath(framePath)
                            .setType(ScreenshotType.PNG)
                            .setOmitBackground(false)
                    )

                    page.close()
                }
            }

            if (pixelate) {
                return pixelatePng(framePath, tempDir)
            }

            return framePath
        } catch (e: Exception) {
            System.err.println("Error in generateComposite: ${e.message}") // Fixed log message name
            throw e
        }
    }

    private fun pixelatePng(inputPng: Path, tempDir: Path): Path {
        val pixelatedPngPath = tempDir.resolve("frame_000_pixelated.png")

        executeCmd(
            "magick",
            inputPng.absolutePathString(),
            "-scale", "20%",
            "-scale", "500%",
            pixelatedPngPath.absolutePathString()
        )

        return pixelatedPngPath
    }
}