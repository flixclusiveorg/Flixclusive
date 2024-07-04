package com.flixclusive.model.datastore.player

import com.flixclusive.core.util.common.ui.UiText
import com.flixclusive.model.provider.SourceLink
import com.flixclusive.core.util.R as UtilR

enum class PlayerQuality(
    val qualityName: UiText,
    val regex: Regex
) {
    Quality8k(
        qualityName = UiText.StringValue("8K"),
        regex = Regex("8k|4320p|4320|ultrahd|uhd", RegexOption.IGNORE_CASE)
    ),
    Quality4k(
        qualityName = UiText.StringValue("4K"),
        regex = Regex("4k|2160p|2160|ultrahd|uhd", RegexOption.IGNORE_CASE)
    ),
    Quality1440p(
        qualityName = UiText.StringValue("1440p"),
        regex = Regex("1440p|1440|2k|quadhd|qhd", RegexOption.IGNORE_CASE)
    ),
    Quality1080p(
        qualityName = UiText.StringValue("1080p"),
        regex = Regex("1080p|1080|fullhd|fhd", RegexOption.IGNORE_CASE)
    ),
    Quality720p(
        qualityName = UiText.StringValue("720p"),
        regex = Regex("720p|720|hd", RegexOption.IGNORE_CASE)
    ),
    Quality480p(
        qualityName = UiText.StringValue("480p"),
        regex = Regex("480p|480", RegexOption.IGNORE_CASE)
    ),
    Quality360p(
        qualityName = UiText.StringValue("360p"),
        regex = Regex("360p|360", RegexOption.IGNORE_CASE)
    ),
    Quality240p(
        qualityName = UiText.StringValue("240p"),
        regex = Regex("240p|240", RegexOption.IGNORE_CASE)
    ),
    Quality144p(
        qualityName = UiText.StringValue("144p"),
        regex = Regex("144p|144", RegexOption.IGNORE_CASE)
    ),
    QualityAuto(
        qualityName = UiText.StringResource(UtilR.string.auto_option),
        regex = Regex("auto|hls|dash", RegexOption.IGNORE_CASE)
    );

    companion object {
        fun List<SourceLink>.getIndexOfPreferredQuality(preferredQuality: PlayerQuality): Int {
            val preferredQualityIndex = indexOfFirst {
                preferredQuality.regex.matches(it.name) || preferredQuality.regex.matches(it.url)
            }


            if (preferredQualityIndex != -1) {
                return preferredQualityIndex
            }

            return entries.firstNotNullOfOrNull { quality ->
                val index = indexOfFirst {
                    quality.regex.matches(it.name) || quality.regex.matches(it.url)
                }

                if (index != -1) index else null
            } ?: 0
        }
    }
}