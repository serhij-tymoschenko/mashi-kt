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
import java.util.*

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
                Browser.NewContextOptions()
                    .setViewportSize(ViewportSize(PNG_WIDTH, PNG_HEIGHT))
                    .setDeviceScaleFactor(1.0)
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


                // Freeze frame by drawing each image onto a smooth-scaled canvas asynchronously
                page.evaluate(
                    """
    async (args) => {
        const images = Array.from(document.querySelectorAll('img'));
        
        const processImage = (img) => {
            return new Promise((resolve) => {
                const ratio = img.naturalWidth / img.naturalHeight;
                
                let targetW = args.IMAGE_WIDTH;
                let targetH = args.IMAGE_HEIGHT;

                if (Math.abs(ratio - 0.75) > 0.01) {
                    targetW = args.TRAIT_WIDTH;
                    targetH = args.TRAIT_HEIGHT;
                }

                // Skip upscaling if the image is already at or above target resolution
                if (img.naturalWidth >= targetW && img.naturalHeight >= targetH) {
                    resolve();
                    return;
                }

                const canvas = document.createElement('canvas');
                canvas.width = targetW;
                canvas.height = targetH;

                const ctx = canvas.getContext('2d');
                
                ctx.imageSmoothingEnabled = true;
                ctx.webkitImageSmoothingEnabled = true;
                ctx.mozImageSmoothingEnabled = true;
                ctx.msImageSmoothingEnabled = true;
                ctx.imageSmoothingQuality = 'high';

                ctx.drawImage(img, 0, 0, img.naturalWidth, img.naturalHeight, 0, 0, targetW, targetH);

                // Wait for the new src to finish decoding/loading before resolving
                img.onload = () => resolve();
                img.onerror = () => resolve(); // Prevent hanging if load fails
                
                img.src = canvas.toDataURL('image/png');
            });
        };

        await Promise.all(images.map(processImage));
    }
    """.trimIndent(), getPngArgs()
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