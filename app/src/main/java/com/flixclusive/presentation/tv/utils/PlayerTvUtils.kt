package com.flixclusive.presentation.tv.utils

import com.flixclusive.presentation.utils.PlayerUiUtils.SECONDS_TO_SEEK
import com.flixclusive.presentation.utils.PlayerUiUtils.SECONDS_TO_SEEK_ON_STREAK1
import com.flixclusive.presentation.utils.PlayerUiUtils.SECONDS_TO_SEEK_ON_STREAK2
import com.flixclusive.presentation.utils.PlayerUiUtils.SECONDS_TO_SEEK_ON_STREAK3

object PlayerTvUtils {
    fun getTimeToSeekToBasedOnSeekMultiplier(
        currentTime: Long,
        maxDuration: Long,
        seekMultiplier: Long
    ): Long {
        var timeToSeekTo = if(seekMultiplier in 15..25) {
            currentTime + (seekMultiplier * SECONDS_TO_SEEK_ON_STREAK1)
        } else if(seekMultiplier in 26..35) {
            currentTime + (seekMultiplier * SECONDS_TO_SEEK_ON_STREAK2)
        } else if(seekMultiplier > 36) {
            currentTime + (seekMultiplier * SECONDS_TO_SEEK_ON_STREAK3)
        } else {
            currentTime + (seekMultiplier * SECONDS_TO_SEEK)
        }

        if(timeToSeekTo > maxDuration)
            timeToSeekTo = maxDuration
        else if(timeToSeekTo < 0)
            timeToSeekTo = 0

        return timeToSeekTo
    }
}