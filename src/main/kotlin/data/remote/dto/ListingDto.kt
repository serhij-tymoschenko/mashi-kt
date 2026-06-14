package com.mashiverse.data.remote.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ListingDto(
    val success: Boolean,
    val listing: ListingDetails
) {
    @Serializable
    data class ListingDetails(
        val id: String,
        val listingId: String,
        val title: String,
        val artistName: String,
        val artistWallet: String,
        val description: String,
        val price: String,
        val currency: String,
        val maxSupply: Int,
        val totalSold: Int,
        val isSoldOut: Boolean,
        val maxPerWallet: Int,
        val status: String,
        val paused: Boolean,
        val chainId: Int,
        val marketplace: String,
        val images: ImagePaths,
        val tokenURI: String,
        val createdAt: JsonElement?, // Handled as flexible JsonElement since it can be null
        val metadata: Metadata
    ) {
        @Serializable
        data class ImagePaths(
            val composite: String,
            val thumbnail: String
        )

        @Serializable
        data class Metadata(
            val name: String,
            val image: String,
            val description: String,
            val attributes: List<Attribute>,
            val assets: List<MetadataAsset>
        ) {
            @Serializable
            data class Attribute(
                val trait_type: String, // Kept snake_case to match incoming JSON keys
                val value: String
            )

            @Serializable
            data class MetadataAsset(
                val label: String,
                val uri: String
            )
        }
    }
}





