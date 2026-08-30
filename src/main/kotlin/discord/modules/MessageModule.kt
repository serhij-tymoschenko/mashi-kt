package com.mashiverse.discord.modules

import com.mashiverse.data.remote.dto.NotifyDto
import dev.kord.common.Color
import dev.kord.rest.builder.message.EmbedBuilder

private fun String.toHttpIpfsUrl(): String = this
    .replace("ipfs://", "https://round-peach-hippopotamus.myfilebase.com/ipfs/")

private fun getAssetLinkList(assets: NotifyDto.AssetsDto): List<String> {
    return listOf(
        "eyes" to assets.eyes,
        "head" to assets.head,
        "upper" to assets.upper,
        "bottom" to assets.bottom,
        "cape" to assets.cape,
        "hair_back" to assets.hairBack,
        "hair_front" to assets.hairFront,
        "hat" to assets.hat,
        "left_acc" to assets.leftAccessory,
        "right_acc" to assets.rightAccessory,
        "background" to assets.background
    ).mapNotNull { (key, value) ->
        if (!value.isNullOrBlank()) {
            "[$key](${value.toHttpIpfsUrl()})"
        } else null
    }
}

fun getNotifyEmbed(data: NotifyDto, isRelease: Boolean): EmbedBuilder {
    val urlStr = if (isRelease && data.listing != null) {
        "https://mash-it.io/mashers?listing=${data.listing.listingId}"
    } else {
        "https://mash-it.io/mashers"
    }

    val details = if (isRelease && data.listing != null) {
        """
        Artist: ${data.artistName}
        Price: ${data.listing.priceMatic} USDC
        Max Supply: ${data.listing.maxSupply}
        Max Per-Wallet: ${data.listing.maxPerWallet}
        """.trimIndent()
    } else {
        "Artist: ${data.artistName}"
    }

    val assetLinks = getAssetLinkList(data.assets)

    return EmbedBuilder().apply {
        title = data.title
        url = urlStr
        color = Color(0x00FF00) // Green
        image = data.assets.composite.toHttpIpfsUrl()

        field {
            name = "Details"
            value = details
            inline = false
        }

        // Split assets into 2 fields to safely stay well below the 1024 limit
        if (assetLinks.isNotEmpty()) {
            val chunks = assetLinks.chunked((assetLinks.size + 1) / 2)

            chunks.forEachIndexed { index, chunk ->
                field {
                    name = if (index == 0) "Assets" else "Assets (cont.)"
                    value = chunk.chunked(3).joinToString("\n") { it.joinToString(" · ") }
                    inline = true
                }
            }
        }

        footer {
            text = "© 2026 mash-it x ${data.artistName}"
        }
    }
}