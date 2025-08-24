package com.flixclusive.core.presentation.player

/**
 * Interface for providing the current subtitle offset dynamically.
 * This allows decoders to read the latest offset value without being recreated.
 */
interface SubtitleOffsetProvider {
    val currentSubtitleOffset: Long
}
