package com.mashiverse.discord

import com.mashiverse.discord.modules.MashupModule
import com.mashiverse.discord.modules.WalletModule
import com.mashiverse.discord.modules.getNotifyEmbed
import com.mashiverse.configs.*
import com.mashiverse.data.db.daos.ReactionsDao
import com.mashiverse.data.remote.dto.NotifyDto
import com.mashiverse.services.NotificationService.notifyAndroidUsers
import com.mashiverse.services.NotificationService.notifyIosUsers
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.event.message.ReactionAddEvent
import dev.kord.core.event.message.ReactionRemoveEvent
import dev.kord.core.on
import dev.kord.gateway.PrivilegedIntent
import dev.kord.rest.builder.message.allowedMentions
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MashiBot private constructor(val kord: Kord) : KoinComponent {
    private val reactionsDao by inject<ReactionsDao>()

    companion object {
        @Volatile
        private var INSTANCE: MashiBot? = null

        fun initialize(kord: Kord): MashiBot {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MashiBot(kord).also { INSTANCE = it }
            }
        }

        fun getInstance(): MashiBot {
            return INSTANCE ?: throw IllegalStateException(
                "MashiBot has not been initialized. Call initialize(kord) first."
            )
        }
    }

    private fun getPosterIdFromMessage(message: Message): Long? {
        // Priority 1: Interaction user metadata
        message.interaction?.user?.id?.let { return it.value.toLong() }

        // Priority 2: Embed Footer Fallback
        val footerText = message.embeds.firstOrNull()?.footer?.text
        if (footerText != null) {
            try {
                return footerText.substringAfterLast(":").trim().toLong()
            } catch (_: Exception) {
            }
        }
        return null
    }

    @OptIn(PrivilegedIntent::class)
    suspend fun setup() {
        MashupModule(kord)
        WalletModule(kord)

        // Event hooks
        kord.on<ReactionAddEvent> {
            if (emoji.name != "🔥" || userId == kord.getSelf().id) return@on

            try {
                val msg = message.asMessage()
                if (!msg.author?.isBot!!) return@on

                val posterId = getPosterIdFromMessage(msg)
                if (posterId != null && posterId != userId.value.toLong()) {
                    reactionsDao.updateReactionsCount(posterId, 1)
                    val totalCount = reactionsDao.getReactionsCount(posterId)

                    if (totalCount > 0 && totalCount % 25 == 0) {
                        val channel = msg.getChannel() as TextChannel
                        channel.createMessage("🔥! <@$posterId> just hit $totalCount reactions!")
                    }
                }
            } catch (_: Exception) {
            }
        }

        kord.on<ReactionRemoveEvent> {
            if (emoji.name != "🔥" || userId == kord.getSelf().id) return@on

            try {
                val msg = message.asMessage()
                if (!msg.author?.isBot!!) return@on

                val posterId = getPosterIdFromMessage(msg)
                if (posterId != null && posterId != userId.value.toLong()) {
                    reactionsDao.updateReactionsCount(posterId, -1)
                }
            } catch (_: Exception) {
            }
        }
    }

    suspend fun notify(data: NotifyDto, isRelease: Boolean = true) {
        try {
            val embedBuilder = getNotifyEmbed(data, isRelease)
            val channelId = Snowflake(if (isRelease) RELEASES_CHANNEL_ID else APPROVALS_CHANNEL_ID)
            val channel = kord.getChannelOf<TextChannel>(channelId) ?: return

            val roleId = Snowflake(if (isRelease) RELEASES_ROLE_ID else APPROVALS_ROLE_ID)

            channel.createMessage {
                content = "<@&$roleId>"
                embeds?.add(embedBuilder)
                allowedMentions { roles.add(roleId) }
            }

            if (isRelease && data.listing != null) {
                val listing = data.listing
                val priceMatic = listing.priceMatic
                val maxSupply = listing.maxSupply
                val maxPerWallet = listing.maxPerWallet
                val docId = data.docId

                val androidTitle = "${data.title} by ${data.artistName} is out"
                val androidBody = "Price: ${priceMatic}USDC\nSupply: $maxSupply\nMax per-wallet: $maxPerWallet"

                try {
                    notifyAndroidUsers(title = androidTitle, body = androidBody, listingId = docId)
                    notifyIosUsers(title = androidTitle, body = androidBody, listingId = docId)
                } catch (e: Exception) {
                    println(e.message)
                }

                //TODO: fetchAndCacheAsync(docId)
            }

        } catch (e: Exception) {
            println(e)
            val testChannel = kord.getChannelOf<TextChannel>(Snowflake(TEST_CHANNEL_ID))
            testChannel?.createMessage("Notify: ${e.message} for $data")
        }
    }
}