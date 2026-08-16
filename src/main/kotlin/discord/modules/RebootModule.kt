package com.mashiverse.discord.modules

import dev.kord.common.entity.Permission
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.core.on
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RebootModule(private val kord: Kord) {
    init {
        registerCommands()
        listenToInteractions()
    }

    private fun registerCommands() {
        kord.launch {
            kord.createGlobalChatInputCommand("reboot", "Reboots Mashi bot VPS") {
                dmPermission = false
            }
        }
    }

    private fun listenToInteractions() {
        kord.on<ChatInputCommandInteractionCreateEvent> {
            when (interaction.command.rootName) {
                "reboot" -> handleReboot(this)
            }
        }
    }

    private suspend fun handleReboot(event: ChatInputCommandInteractionCreateEvent) {
        val user = event.interaction.user
        val isAuthorizedUser = user.id == Snowflake("1167694222120468553")

        val guildId = event.interaction.data.guildId.value
        val guild = guildId?.let { kord.getGuildOrNull(it) }
        val member = guildId?.let { user.asMemberOrNull(it) }

        val isOwner = guild?.ownerId == user.id
        val isAdmin = member?.getPermissions()?.contains(Permission.Administrator) == true

        if (!(isOwner || isAdmin || isAuthorizedUser)) {
            event.interaction.respondEphemeral {
                content = "❌ You do not have permission to reboot the system."
            }
            return
        }

        val response = event.interaction.deferEphemeralResponse()
        response.respond {
            content = "🔄 Initiating VPS reboot now..."
        }

        withContext(Dispatchers.IO) {
            ProcessBuilder("sudo", "reboot")
                .inheritIO()
                .start()
        }
    }
}