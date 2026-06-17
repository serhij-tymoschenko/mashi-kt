package com.mashiverse.discord.modules

import com.mashiverse.data.remote.dto.NotifyDto
import dev.kord.common.Color
import dev.kord.rest.builder.message.EmbedBuilder

private fun String.toHttpIpfsUrl(): String = this.replace("ipfs://", "https://ipfs.io/ipfs/")

fun generateAssetsLinks(assets: NotifyDto.AssetsDto): String {
    val assetsList = listOf(
        "eyes" to assets.eyes,
        "head" to assets.head,
        "upper" to assets.upper,
        "bottom" to assets.bottom,
        "cape" to assets.cape,
        "hair_back" to assets.hairBack,
        "hair_front" to assets.hairFront,
        "hat" to assets.hat,
        "left_accessory" to assets.leftAccessory,
        "right_accessory" to assets.rightAccessory,
        "background" to assets.background
    ).mapNotNull { (key, value) ->
        if (value != null) {
            "[$key](${value.toHttpIpfsUrl()})"
        } else null
    }

    val rows = assetsList.chunked(3).map { row ->
        row.joinToString(" · ")
    }
    return rows.joinToString("\n")
}

fun getNotifyEmbed(data: NotifyDto, isRelease: Boolean): EmbedBuilder {
    val urlStr = if (isRelease && data.listing != null) {
        "https://mash-it.io/mashers?listing=${data.listing.listingId}"
    } else {
        "https://mash-it.io/mashers"
    }

    val embed = EmbedBuilder().apply {
        title = data.title
        url = urlStr
        color = Color(0x00FF00) // Green
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

    embed.field {
        name = "Details"
        value = details
        inline = false
    }

    embed.field {
        name = "Assets:"
        value = generateAssetsLinks(data.assets)
        inline = false
    }

    embed.image = data.assets.composite.toHttpIpfsUrl()

    embed.footer {
        text = "© 2026 mash-it x ${data.artistName}"
    }

    return embed
}