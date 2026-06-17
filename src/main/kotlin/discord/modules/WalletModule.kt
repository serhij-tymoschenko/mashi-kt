package com.mashiverse.discord.modules

import com.mashiverse.configs.TEST_CHANNEL_ID
import com.mashiverse.data.db.daos.UserDao
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.interaction.string
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class WalletModule(private val kord: Kord) : KoinComponent {
    private val userDao by inject<UserDao>()

    init {
        registerCommands()
        listenToInteractions()
    }

    private fun registerCommands() {
        kord.launch {
            kord.createGuildChatInputCommand(Snowflake(1419701957995659317), "connect_wallet", "Connect wallet") {
                string("wallet", "Wallet") { required = true }
            }
            kord.createGuildChatInputCommand(Snowflake(1419701957995659317), "disconnect_wallet", "Disconnect wallet")
        }
    }

    private fun listenToInteractions() {
        kord.on<GuildChatInputCommandInteractionCreateEvent> {
            when (interaction.command.rootName) {
                "connect_wallet" -> handleConnectWallet(this)
                "disconnect_wallet" -> handleDisconnectWallet(this)
            }
        }
    }

    private suspend fun handleConnectWallet(event: GuildChatInputCommandInteractionCreateEvent) {
        val interaction = event.interaction
        val wallet = interaction.command.options["wallet"]!!.value.toString()
        val response = interaction.deferEphemeralResponse()

        try {
            if (wallet.length != 42) {
                response.respond { content = "Invalid wallet" }
                return
            }

            val userId = interaction.user.id.value.toLong()
            val hasWallet = userDao.getWallet(userId) != null
            if (hasWallet) {
                response.respond { content = "You already have wallet" }
                return
            }

            val isAnotherUserWallet = userDao.isExist(wallet)
            if (isAnotherUserWallet) {
                response.respond { content = "Wallet already taken" }
                return
            }

            userDao.connectWallet(userId, wallet.lowercase())
            response.respond { content = "Wallet connected" }

        } catch (e: Exception) {
            println(e)
            response.respond { content = "Something went wrong" }
        }
    }

    private suspend fun handleDisconnectWallet(event: GuildChatInputCommandInteractionCreateEvent) {
        val interaction = event.interaction
        val response = interaction.deferEphemeralResponse()

        try {
            userDao.disconnectWallet(interaction.user.id.value.toLong())
            response.respond { content = "Wallet disconnected" }
        } catch (e: Exception) {
            println(e)
            response.respond { content = "Something went wrong" }
        }
    }
}