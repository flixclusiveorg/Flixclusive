package com.flixclusive.core.presentation.player.model

import androidx.annotation.OptIn
import androidx.compose.runtime.Stable
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.text.CuesWithTiming

@Stable
data class CueWithTiming(
    val cue: List<String>,
    val startTimeMs: Long,
    val durationMs: Long,
    val endTimeMs: Long
) {
    companion object {
        @OptIn(UnstableApi::class)
        fun CuesWithTiming.toCue(): CueWithTiming {
            return CueWithTiming(
                cue = cues.map { it.text.toString() },
                startTimeMs = startTimeUs,
                durationMs = durationUs,
                endTimeMs = endTimeUs
            )
        }
    }
}
