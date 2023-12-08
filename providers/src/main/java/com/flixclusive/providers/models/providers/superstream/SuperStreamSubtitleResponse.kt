package com.flixclusive.providers.models.providers.superstream

import com.flixclusive.providers.sources.superstream.SuperStreamCommon.captionDomains
import com.google.gson.annotations.SerializedName

internal data class SuperStreamSubtitleResponse(
    val code: Int? = null,
    val msg: String? = null,
    val data: SubtitleData = SubtitleData()
) {
    data class SuperStreamSubtitleItem(
        @SerializedName("file_path") val filePath: String? = null,
        val lang: String? = null,
        val language: String? = null,
        val order: Int? = null,
    ) {
        companion object {
            fun String.toValidSubtitleFilePath(): String {
                return replace(captionDomains[0], captionDomains[1])
                    .replace(Regex("\\s"), "+")
                    .replace(Regex("[()]")) { result ->
                        "%" + result.value.toCharArray()[0].code.toByte().toString(16)
                    }
            }
        }
    }

    data class SuperStreamSubtitle(
        val language: String? = null,
        val subtitles: List<SuperStreamSubtitleItem> = listOf()
    )

    data class SubtitleData(
        val select: List<String> = listOf(),
        val list: List<SuperStreamSubtitle> = listOf()
    )
}