package com.mashiverse.data.di

import com.mashiverse.data.db.daos.ImageDao
import com.mashiverse.data.db.daos.ReactionsDao
import com.mashiverse.data.db.daos.UserDao
import com.mashiverse.data.db.entities.User
import com.mashiverse.data.remote.KtorClient
import com.mashiverse.data.remote.apis.IpfsApi
import com.mashiverse.data.repos.ImageRepo
import com.mashiverse.images.playwright.combiners.AnimCombiner
import com.mashiverse.images.playwright.combiners.CompositeCombiner
import images.services.ImageService
import io.ktor.client.HttpClient
import io.ktor.server.application.*
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.configureKoin() {
    install(Koin) {
        slf4jLogger()
        modules(module {
            single<AnimCombiner> {
                AnimCombiner()
            }

            single<ImageRepo> {
                ImageRepo()
            }

            single<ImageService> {
                ImageService()
            }

            factory<UserDao> {
                UserDao()
            }

            factory<ReactionsDao> {
                ReactionsDao()
            }

            factory<ImageDao> {
                ImageDao()
            }

            single<CompositeCombiner> {
                CompositeCombiner()
            }

            factory<IpfsApi> {
                IpfsApi()
            }

            single<HttpClient> {
                KtorClient.client()
            }

            single<ImageRepo>{
                ImageRepo()
            }
        })
    }
}
