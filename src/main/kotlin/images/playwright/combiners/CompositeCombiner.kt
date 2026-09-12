package com.mashiverse.images.playwright.combiners

import com.mashiverse.configs.PNG_HEIGHT
import com.mashiverse.configs.PNG_WIDTH
import com.mashiverse.images.playwright.PlaywrightService
import com.mashiverse.utils.helpers.readImageFiles
import com.microsoft.playwright.Browser
import com.microsoft.playwright.Page
import com.microsoft.playwright.options.LoadState
import com.microsoft.playwright.options.ScreenshotType
import com.microsoft.playwright.options.ViewportSize
import org.koin.core.component.KoinComponent
import java.nio.file.Path

class CompositeCombiner : KoinComponent {
    fun generateComposite(tempDir: Path): Path {
        val browser = PlaywrightService.getBrowser()
        val frameName = String.format("frame_%03d.png", 0)
        val framePath = tempDir.resolve(frameName)

        try {
            val imageUrls = readImageFiles(tempDir)
            val htmlContent = prepareHtml(
                urls = imageUrls,
                width = PNG_WIDTH,
                height = PNG_HEIGHT
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

                    // Wait for initial load and image decodes (LOAD avoids NETWORKIDLE's mandatory 500ms delay)
                    page.waitForLoadState(LoadState.LOAD)
                    page.waitForFunction(
                        "Array.from(document.images).every(img => img.complete && img.naturalWidth > 0)"
                    )

                    // Option 1: Freeze frame by drawing each image onto a canvas and swapping src
                    page.evaluate(
                        """
                        () => {
                            for (const img of document.querySelectorAll('img')) {
                                const canvas = document.createElement('canvas');
                                canvas.width = img.naturalWidth;
                                canvas.height = img.naturalHeight;
                                const ctx = canvas.getContext('2d');
                                ctx.drawImage(img, 0, 0);
                                img.src = canvas.toDataURL();
                            }
                        }
                        """.trimIndent()
                    )

                    // Ensure all swapped data-URL images are completed rendering
                    page.waitForFunction(
                        "Array.from(document.images).every(img => img.complete)"
                    )

                    // Capture immediately now that images are converted to static canvases
                    page.screenshot(
                        Page.ScreenshotOptions()
                            .setPath(framePath)
                            .setType(ScreenshotType.PNG)
                            .setOmitBackground(false)
                    )

                    page.close()
                }
            }

            return framePath
        } catch (e: Exception) {
            System.err.println("Error in generateComposite: ${e.message}")
            throw e
        }
    }
}