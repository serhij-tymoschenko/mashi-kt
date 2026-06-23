package data.remote.dto

import com.mashiverse.data.models.Asset
import com.mashiverse.data.models.Colors
import kotlinx.serialization.Serializable

@Serializable
data class MashupDto(
    val wallet: String? = null,
    val id: String? = null,
    val timestamp: Long? = null,
    val colors: Colors,
    val count: Int? = null,
    val assets: List<Asset>
)