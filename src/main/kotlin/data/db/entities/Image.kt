package com.mashiverse.data.db.entities

data class Image(
    val url: String,
    val data: ByteArray? = null,
    val webpData: ByteArray? = null,
    val svgData: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Image

        if (url != other.url) return false
        if (!data.contentEquals(other.data)) return false
        if (!webpData.contentEquals(other.webpData)) return false
        if (!svgData.contentEquals(other.svgData)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = url.hashCode()
        result = 31 * result + (data?.contentHashCode() ?: 0)
        result = 31 * result + (webpData?.contentHashCode() ?: 0)
        result = 31 * result + (svgData?.contentHashCode() ?: 0)
        return result
    }
}
