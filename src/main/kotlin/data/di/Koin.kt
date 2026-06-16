package com.mashiverse.data.di

import com.mashiverse.data.remote.KtorClient
import com.mashiverse.images.playwright.PlaywrightService
import com.mashiverse.images.playwright.combiners.AnimCombiner
import com.mashiverse.images.playwright.combiners.CompositeCombiner
import com.microsoft.playwright.Browser
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

            single<AnimCombiner> {
                AnimCombiner()
            }

            single<CompositeCombiner> {
                CompositeCombiner()
            }

            single<HttpClient> {
                KtorClient.client()
            }
        })
    }
}
