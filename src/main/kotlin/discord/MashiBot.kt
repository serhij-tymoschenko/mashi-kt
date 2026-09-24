package com.mashiverse.discord

import com.mashiverse.configs.*
import com.mashiverse.data.remote.dto.NotifyDto
import com.mashiverse.discord.modules.MashupModule
import com.mashiverse.discord.modules.RebootModule
import com.mashiverse.discord.modules.WalletModule
import com.mashiverse.discord.modules.getNotifyEmbed
import com.mashiverse.services.AnimService
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.TextChannel
import dev.kord.rest.builder.message.allowedMentions
import dev.kord.rest.builder.message.embed
import io.ktor.client.request.forms.*
import io.ktor.utils.io.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MashiBot private constructor(val kord: Kord) : KoinComponent {
    private val animService by inject<AnimService>()

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

    fun setup() {
        MashupModule(kord)
        WalletModule(kord)
        RebootModule(kord)
    }

    suspend fun notify(data: NotifyDto, isRelease: Boolean = true) {
        try {
            val channelId = Snowflake(if (isRelease) RELEASES_CHANNEL_ID else APPROVALS_CHANNEL_ID)
            val channel = kord.getChannelOf<TextChannel>(channelId) ?: return

            val roleId = Snowflake(if (isRelease) RELEASES_ROLE_ID else APPROVALS_ROLE_ID)

            try {
                val isAnyAnimated = animService.checkIfAnyAnimated(data)
                if (!isAnyAnimated) {
                    channel.createMessage {
                        content = "<@&$roleId>"

                        embed {
                            val builtEmbed = getNotifyEmbed(data, isRelease = isRelease)
                            title = builtEmbed.title
                            url = builtEmbed.url
                            color = builtEmbed.color
                            image = builtEmbed.image ?: data.assets.composite.replace(
                                "ipfs://",
                                "https://ipfs.filebase.io/ipfs/"
                            )
                            footer = builtEmbed.footer
                            fields = builtEmbed.fields
                        }

                        allowedMentions {
                            roles.add(roleId)
                        }
                    }
                } else {
                    val anim = animService.generateAnim(data)
                    if (anim != null) {
                        val fileName = "embed_image.gif"

                        channel.createMessage {
                            content = "<@&$roleId>"

                            addFile(
                                name = fileName,
                                contentProvider = ChannelProvider(size = anim.size.toLong()) {
                                    ByteReadChannel(anim)
                                }
                            )

                            embed {
                                val builtEmbed = getNotifyEmbed(data, isRelease)
                                title = builtEmbed.title
                                url = builtEmbed.url
                                color = builtEmbed.color
                                image = "attachment://$fileName"
                                footer = builtEmbed.footer
                                fields = builtEmbed.fields
                            }

                            allowedMentions {
                                roles.add(roleId)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
            }
        } catch (e: Exception) {
            println(e)
            val testChannel = kord.getChannelOf<TextChannel>(Snowflake(TEST_CHANNEL_ID))
            testChannel?.createMessage("Notify: ${e.message} for $data")
        }
    }
}