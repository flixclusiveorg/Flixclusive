package com.flixclusive.core.presentation.player

import com.flixclusive.core.presentation.player.model.CueWithTiming

internal interface CuesProvider {
    fun addCue(cue: CueWithTiming)

    fun clearCues()
}
