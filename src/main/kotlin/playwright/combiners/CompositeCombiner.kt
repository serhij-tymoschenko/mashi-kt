package com.mashiverse.playwright.combiners

import com.google.gson.JsonObject
import com.mashiverse.configs.PNG_HEIGHT
import com.mashiverse.configs.PNG_WIDTH
import com.mashiverse.utils.readImageFiles
import com.microsoft.playwright.Browser
import com.microsoft.playwright.Page
import com.microsoft.playwright.options.ViewportSize
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.nio.file.Paths

class CompositeCombiner : KoinComponent {
    val browser by inject<Browser>()

    fun generateComposite(tempDir: String) {
        val context = browser.newContext(
            Browser.NewContextOptions().setViewportSize(ViewportSize(PNG_WIDTH, PNG_HEIGHT))
        )

        try {
            val imageUrls = readImageFiles(tempDir)
            val htmlContent = prepareHtml(
                urls = imageUrls,
                width = PNG_WIDTH,
                height = PNG_HEIGHT
            )

            val page = context.newPage()
            page.setContent(htmlContent)
            preparePage(page, getPngArgs())

            val client = page.context().newCDPSession(page)

            val initialPolicyArgs = JsonObject().apply {
                addProperty("policy", "advance")
                addProperty("budget", 0)
            }

            client.send(
                "Emulation.setVirtualTimePolicy",
                initialPolicyArgs
            )

            val frameName = String.format("frame_%03d.png", 0)
            val framePath = Paths.get(tempDir, frameName)

            page.screenshot(
                Page.ScreenshotOptions()
                    .setPath(framePath)
                    .setType(com.microsoft.playwright.options.ScreenshotType.PNG)
                    .setOmitBackground(false)
            )

            page.close()
        } catch (e: Exception) {
            System.err.println("Error in generateGif: ${e.message}")
            throw e
        } finally {
            context.close()
        }
    }
}

