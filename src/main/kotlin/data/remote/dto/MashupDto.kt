package data.remote.dto

import kotlinx.serialization.Serializable


@Serializable
data class MashupDto(
    val wallet: String,
    val id: String,
    val timestamp: Long,
    val colors: Colors,
    val count: Int,
    val assets: List<Asset>
) {
    @Serializable
    data class Asset(
        val name: String,
        val image: String
    )

    @Serializable
    data class Colors(
        val base: String,
        val eyes: String,
        val hair: String
    )
}