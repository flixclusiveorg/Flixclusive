package com.flixclusive.feature.mobile.settings.util

import androidx.compose.runtime.Stable
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.datastore.model.user.player.CaptionEdgeTypePreference
import com.flixclusive.core.datastore.model.user.player.DecoderPriority
import com.flixclusive.core.datastore.model.user.player.PlayerQuality
import com.flixclusive.core.datastore.model.user.player.ResizeMode

/**
 * Extension property to convert enum values to [UiText] for localization.
 * */
@Stable
internal val <T> T.uiText: UiText
    get() {
        return when (this) {
            is ResizeMode -> UiText.from(this.getStringId())
            is DecoderPriority -> UiText.from(this.getStringResId())
            is PlayerQuality -> UiText.from(this.qualityStringResId)
            is CaptionEdgeTypePreference -> UiText.from(this.getStringResId())
            else -> throw IllegalArgumentException("Unsupported type")
        }
    }
