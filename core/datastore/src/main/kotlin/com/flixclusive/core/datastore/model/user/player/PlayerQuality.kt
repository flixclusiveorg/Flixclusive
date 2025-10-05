package com.flixclusive.core.datastore.model.user.player

import com.flixclusive.core.datastore.R

enum class PlayerQuality(
    val qualityStringResId: Int,
    val regex: Regex
) {
    Quality8k(
        qualityStringResId = R.string.quality_8k,
        regex = Regex("8k|4320p|4320|ultrahd|uhd", RegexOption.IGNORE_CASE)
    ),
    Quality4k(
        qualityStringResId = R.string.quality_4k,
        regex = Regex("4k|2160p|2160|ultrahd|uhd", RegexOption.IGNORE_CASE)
    ),
    Quality1440p(
        qualityStringResId = R.string.quality_2k,
        regex = Regex("1440p|1440|2k|quadhd|qhd", RegexOption.IGNORE_CASE)
    ),
    Quality1080p(
        qualityStringResId = R.string.quality_1080p,
        regex = Regex("1080p|1080|fullhd|fhd", RegexOption.IGNORE_CASE)
    ),
    Quality720p(
        qualityStringResId = R.string.quality_720p,
        regex = Regex("720p|720|hd", RegexOption.IGNORE_CASE)
    ),
    Quality480p(
        qualityStringResId = R.string.quality_480p,
        regex = Regex("480p|480", RegexOption.IGNORE_CASE)
    ),
    Quality360p(
        qualityStringResId = R.string.quality_360p,
        regex = Regex("360p|360", RegexOption.IGNORE_CASE)
    ),
    Quality240p(
        qualityStringResId = R.string.quality_240p,
        regex = Regex("240p|240", RegexOption.IGNORE_CASE)
    ),
    Quality144p(
        qualityStringResId = R.string.quality_144p,
        regex = Regex("144p|144", RegexOption.IGNORE_CASE)
    ),
    QualityAuto(
        qualityStringResId = R.string.quality_auto,
        regex = Regex("auto|hls|dash", RegexOption.IGNORE_CASE)
    );
}
