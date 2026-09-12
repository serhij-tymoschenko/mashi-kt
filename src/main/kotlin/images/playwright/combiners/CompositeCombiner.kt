package com.mashiverse.images.playwright.combiners

import com.mashiverse.configs.PNG_HEIGHT
import com.mashiverse.configs.PNG_WIDTH
import com.mashiverse.images.playwright.PlaywrightPool
import com.microsoft.playwright.Browser
import com.microsoft.playwright.Page
import com.microsoft.playwright.options.LoadState
import com.microsoft.playwright.options.ScreenshotType
import com.microsoft.playwright.options.ViewportSize
import org.koin.core.component.KoinComponent
import java.util.Base64

class CompositeCombiner : KoinComponent {

    suspend fun generateComposite(traitsBytes: List<Pair<String, ByteArray>>): ByteArray {
        val imageUrls = traitsBytes.map { (mime, bytes) ->
            val b64 = Base64.getEncoder().encodeToString(bytes)
            "data:$mime;base64,$b64"
        }

        val htmlContent = prepareHtml(
            urls = imageUrls,
            width = PNG_WIDTH,
            height = PNG_HEIGHT
        )

        return PlaywrightPool.execute { browser ->
            val context = browser.newContext(
                Browser.NewContextOptions().setViewportSize(ViewportSize(PNG_WIDTH, PNG_HEIGHT))
            )

            context.use { ctx ->
                val page = ctx.newPage()
                page.setContent(htmlContent)
                preparePage(page, getPngArgs())

                // Wait for initial load and image decodes
                page.waitForLoadState(LoadState.LOAD)
                page.waitForFunction(
                    "Array.from(document.images).every(img => img.complete && img.naturalWidth > 0)"
                )

                // Freeze frame by drawing each image onto a canvas and swapping src
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

                // Ensure all swapped data-URL images have completed rendering
                page.waitForFunction(
                    "Array.from(document.images).every(img => img.complete)"
                )

                // Capture directly to in-memory bytes without writing to disk
                page.screenshot(
                    Page.ScreenshotOptions()
                        .setType(ScreenshotType.PNG)
                        .setOmitBackground(false)
                )
            }
        }
    }
}