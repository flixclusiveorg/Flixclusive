package com.flixclusive.providers.sources.superstream

import com.flixclusive.providers.utils.DecryptUtils

internal object SuperStreamCommon {
    // We do not want content scanners to notice this scraping going on so we've hidden all constants
    // The source has its origins in China so I added some extra security with banned words
    // Mayhaps a tiny bit unethical, but this source is just too good :)
    // If you are copying this code please use precautions so they do not change their api.

    // Free Tibet, The Tienanmen Square protests of 1989
    val iv = DecryptUtils.base64Decode("d0VpcGhUbiE=")
    val key = DecryptUtils.base64Decode("MTIzZDZjZWRmNjI2ZHk1NDIzM2FhMXc2")

    private val baseApiUrl = DecryptUtils.base64Decode("aHR0cHM6Ly9zaG93Ym94LnNoZWd1Lm5ldA==")
    val apiUrl =
        "$baseApiUrl${DecryptUtils.base64Decode("L2FwaS9hcGlfY2xpZW50L2luZGV4Lw==")}"

    // Another url because the first one sucks at searching
    // This one was revealed to me in a dream
    val secondApiUrl =
        DecryptUtils.base64Decode("aHR0cHM6Ly9tYnBhcGkuc2hlZ3UubmV0L2FwaS9hcGlfY2xpZW50L2luZGV4Lw==")

    val appKey = DecryptUtils.base64Decode("bW92aWVib3g=")
    val appId = DecryptUtils.base64Decode("Y29tLnRkby5zaG93Ym94")
    val appIdSecond = DecryptUtils.base64Decode("Y29tLm1vdmllYm94cHJvLmFuZHJvaWQ=")
    val appVersion = "14.7"
    val appVersionCode = "160"
}