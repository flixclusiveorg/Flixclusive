package com.flixclusive.feature.mobile.settings.component.dialog.player

internal object Constant {
    val playerBufferLengths =
        mapOf(
            50L to "50s ",
            60L to "1min",
            90L to "1min 30s",
            120L to "2min",
            150L to "2min 30s",
            180L to "3min",
            210L to "3min 30s",
            240L to "4min",
            300L to "5min",
            360L to "6min",
            420L to "7min",
            480L to "8min",
            540L to "9min",
            600L to "10min",
            900L to "15min",
            1200L to "20min",
            1800L to "30min"
        )

    val playerBufferSizes =
        listOf<Long>(
            -1, 10, 20, 30, 40, 50, 60, 70, 80, 90,
            100, 150, 200, 250, 300, 350, 400, 450, 500
        )

    val playerCacheSizes =
        listOf<Long>(
            0, 10, 20, 30, 40, 50, 60, 70, 80, 90,
            100, 150, 200, 250, 300, 350, 400, 450, 500,
            1000, 2000, -1
        )

    val availableSeekIncrementMs
        = listOf(5000L, 10000L, 30000L)
}