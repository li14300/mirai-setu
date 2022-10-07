package moe.ruabbit.mirai.setu

import kotlinx.serialization.Serializable



@Serializable
data class LoliconResponse(
    val error: String = "",
    val data: List<SetuImageInfo>? = null
) {
    @Serializable
    data class SetuImageInfo(
        val pid: Int,
        val p: Int,
        val uid: Int,
        val title: String,
        val author: String,
        val urls: SetuImageUrl,
        val r18: Boolean,
        val width: Int,
        val height: Int,
        val tags: List<String>,
        val ext: String,
        val uploadDate: Long
    ){
        @Serializable
        data class SetuImageUrl(
            val regular: String
        )
    }
}

