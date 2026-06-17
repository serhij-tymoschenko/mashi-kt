package com.mashiverse

import com.mashiverse.discord.MashiBot
import com.mashiverse.mashi.BuildConfig
import dev.kord.core.Kord
import dev.kord.gateway.Intent
import dev.kord.gateway.Intents
import dev.kord.gateway.PrivilegedIntent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(PrivilegedIntent::class)
fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}
