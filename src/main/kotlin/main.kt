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
suspend fun main(args: Array<String>) {
    val token = BuildConfig.DISCORD_TOKEN
    val kord = Kord(token)

    val bot = MashiBot.initialize(kord)
    bot.setup()

    CoroutineScope(Dispatchers.Default).launch {
        kord.login {
            intents = Intents {
                +Intent.Guilds
                +Intent.GuildMembers
                +Intent.GuildMessageReactions
            }
        }
    }

    io.ktor.server.netty.EngineMain.main(args)
}
