package data.remote.dto

import com.mashiverse.data.models.Asset
import com.mashiverse.data.models.Colors
import kotlinx.serialization.Serializable

@Serializable
data class MashupDto(
    val wallet: String,
    val id: String,
    val timestamp: Long,
    val colors: Colors,
    val count: Int,
    val assets: List<Asset>
)