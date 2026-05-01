package com.flixclusive.core.navigation.navigator

import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.common.tv.Episode

interface StartPlayerAction {
    fun play(media: MediaMetadata, episode: Episode? = null)
}
