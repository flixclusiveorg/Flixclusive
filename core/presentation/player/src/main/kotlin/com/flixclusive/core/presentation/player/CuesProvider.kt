package com.flixclusive.core.presentation.player

import com.flixclusive.core.presentation.player.model.CueWithTiming

internal interface CuesProvider {
    /** The current delay or offset in milliseconds. */
    val offset: Long

    fun addCue(cue: CueWithTiming)

    fun clearCues()
}
