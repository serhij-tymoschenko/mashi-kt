package com.mashiverse.discord.modules

// Mocking imports for illustration
import com.mashiverse.configs.TEST_CHANNEL_ID
import com.mashiverse.data.db.daos.UserDao
import data.models.DownloadType
import dev.kord.common.Color
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.Message
import dev.kord.core.entity.ReactionEmoji
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.entity.interaction.GuildChatInputCommandInteraction
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.message.embed
import images.services.ImageService
import io.ktor.client.request.forms.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.ByteArrayInputStream
import kotlin.random.Random

class MashupModule(private val kord: Kord) : KoinComponent {
    private val userDao by inject<UserDao>()
    private val imageService by inject<ImageService>()

    init {
        registerCommands()
        listenToInteractions()
    }

    private fun registerCommands() {
        kord.launch {
            kord.createGlobalChatInputCommand("mashi", "Generates mashup") {
                dmPermission = true
                string("image", "Image type") {
                    choice("PNG", "PNG")
                    choice("GIF", "GIF")
                }
            }
            kord.createGlobalChatInputCommand("delete_mashup", "Deletes mashup") {
                dmPermission = true
                string("msg_id", "Message id on right click") { required = true }
            }
        }
    }

    private fun listenToInteractions() {
        // Generic event type fires for both guild AND DM interactions.
        // The guild-specific event type only fires inside servers, which is
        // why these commands previously failed in DMs.
        kord.on<ChatInputCommandInteractionCreateEvent> {
            val command = interaction.command
            when (command.rootName) {
                "mashi" -> handleMashi(this)
                "delete_mashup" -> handleDeleteMashup(this)
            }
        }
    }

    private suspend fun handleMashi(event: ChatInputCommandInteractionCreateEvent) {
        val interaction = event.interaction
        val imageOpt = interaction.command.options["image"]?.value?.toString() ?: "PNG"

        var msg: Message? = null
        val userId = interaction.user.id.value.toLong()
        val wallet = userDao.getWallet(userId)

        if (wallet == null) {
            interaction.deferEphemeralResponse().respond {
                content = "Please use /connect_wallet command"
            }
            return
        }

        val response = interaction.deferPublicResponse()

        try {
            val downloadType = DownloadType.valueOf(imageOpt)
            val ext = if (downloadType == DownloadType.PNG) ".png" else ".gif"

            val data = imageService.requestComposite(wallet, downloadType = downloadType)
            if (data != null) {
                val filename = "composite$ext"
                val inputStream = ByteArrayInputStream(data)
                val channelProvider = ChannelProvider { inputStream.toByteReadChannel() }

                val randomColor = Color(Random.nextInt(0xFFFFFF))

                val interactionResponse = response.respond {
                    addFile(filename, channelProvider)
                    embed {
                        title = "${interaction.user.username}'s mashup"
                        color = randomColor
                        image = "attachment://$filename"
                        footer { text = "© 2026 mash-it" }
                    }
                }
                msg = interactionResponse.message
            }
        } catch (e: Exception) {
            // Notifying a fixed log channel still works fine from a DM context,
            // since this uses the bot's own channel lookup, not the user's guild.
            val channel = kord.getChannelOf<TextChannel>(Snowflake(TEST_CHANNEL_ID))
            channel?.createMessage("/mashi: ${e.message}")
            response.respond { content = "Something went wrong" }
        } finally {
            msg?.let {
                try {
                    it.addReaction(ReactionEmoji.Unicode("🔥"))
                } catch (e: Exception) {
                    println(e.message)
                }
            }
        }
    }

    private suspend fun handleDeleteMashup(event: ChatInputCommandInteractionCreateEvent) {
        val interaction = event.interaction
        val msgIdStr = interaction.command.options["msg_id"]!!.value.toString()
        val response = interaction.deferEphemeralResponse()

        try {
            val channel = interaction.channel.asChannel() as TextChannel
            val message = channel.getMessage(Snowflake(msgIdStr.toLong()))

            // Checking interaction metadata
            val metadataUser = message.interaction?.user
            val originalPosterId = metadataUser?.id

            // guildId is null in a DM — only check guild staff permissions when one exists.
            val isStaff = when (val i = interaction) {
                is GuildChatInputCommandInteraction -> {
                    val member = i.user.asMember(i.guildId)
                    val permissions = member.getPermissions()
                    permissions.contains(Permission.Administrator) ||
                            permissions.contains(Permission.ManageMessages) ||
                            i.user.id == i.getGuild()?.ownerId
                }
                else -> false
            }

            // In a DM, channel.getMessage already implicitly restricts this to
            // messages the bot can see in that DM, so the "original poster" check
            // alone is sufficient — there's no staff concept without a guild.
            if (originalPosterId == interaction.user.id || isStaff) {
                message.delete()
                response.respond { content = "Mashup was deleted" }
                return
            }

            response.respond { content = "You are not allowed to delete that mashup" }
        } catch (e: Exception) {
            println(e)
            response.respond { content = "Something went wrong" }
        }
    }
}