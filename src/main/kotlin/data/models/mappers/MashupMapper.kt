package data.models.mappers

import com.mashiverse.data.models.Mashup
import data.remote.dto.MashupDto

fun MashupDto.toMashup() = Mashup(
    colors = this.colors,
    traits = this.assets
)