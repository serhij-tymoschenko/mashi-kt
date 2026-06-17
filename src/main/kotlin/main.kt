package com.mashiverse

import io.ktor.server.engine.*
import io.ktor.server.application.*
import org.koin.plugin.module.dsl.startKoin

fun main(args: Array<String>) {

    io.ktor.server.netty.EngineMain.main(args)
}
