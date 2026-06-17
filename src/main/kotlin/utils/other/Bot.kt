package com.mashiverse.utils.other

import com.mashiverse.configs.DISCORD_TOKEN
import com.mashiverse.discord.MashiBot
import dev.kord.core.Kord
import dev.kord.gateway.Intent
import dev.kord.gateway.Intents
import dev.kord.gateway.PrivilegedIntent
import io.ktor.server.application.*
import kotlinx.coroutines.launch

@OptIn(PrivilegedIntent::class)
fun Application.bot() {
    val token = DISCORD_TOKEN
    launch {
        val kord = Kord(token)
        val bot = MashiBot.initialize(kord)
        bot.setup()

        kord.login {
            intents = Intents {
                +Intent.Guilds
                +Intent.GuildMembers
                +Intent.GuildMessageReactions
            }
        }
    }
}