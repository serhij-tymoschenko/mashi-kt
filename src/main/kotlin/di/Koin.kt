package com.mashiverse.di

import com.mashiverse.playwright.PlaywrightService
import com.microsoft.playwright.Browser
import dev.kord.core.Kord
import io.ktor.server.application.*
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.configureKoin() {
    install(Koin) {
        slf4jLogger()
        modules(module {
            single<Browser> {
                PlaywrightService.getBrowser()
            }
        })
    }
}
