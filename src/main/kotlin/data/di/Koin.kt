package com.mashiverse.data.di

import com.mashiverse.data.remote.KtorClient
import com.mashiverse.playwright.PlaywrightService
import com.microsoft.playwright.Browser
import dev.kord.core.Kord
import io.ktor.client.HttpClient
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

            single<HttpClient> {
                KtorClient.client()
            }
        })
    }
}
