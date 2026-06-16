package com.mashiverse.images.playwright

import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Playwright

class PlaywrightService {

    companion object {
        fun getBrowser(): Browser {
            val playwright = Playwright.create()

            val launchArgs = listOf(
                "--disable-gpu",
                "--disable-software-rasterizer",
                "--disable-gpu-compositing",
                "--disable-gpu-rasterization",
                "--headless=new"
            )

            val launchOptions = BrowserType.LaunchOptions()
                .apply {
                    args = launchArgs
                }

            val browser = playwright.chromium().launch(launchOptions)

            return browser
        }
    }
}