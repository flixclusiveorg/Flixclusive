package com.flixclusive.core.datastore.model.user

import android.graphics.Color
import com.flixclusive.core.datastore.model.user.player.CaptionEdgeTypePreference
import com.flixclusive.core.datastore.model.user.player.CaptionStylePreference
import kotlinx.serialization.Serializable

@Serializable
data class SubtitlesPreferences(
    val isSubtitleEnabled: Boolean = true,
    val subtitleLanguage: String = "en",
    val subtitleColor: Int = Color.WHITE,
    val subtitleSize: Float = 20F,
    val subtitleFontStyle: CaptionStylePreference = CaptionStylePreference.Bold,
    val subtitleBackgroundColor: Int = Color.TRANSPARENT,
    val subtitleEdgeType: CaptionEdgeTypePreference = CaptionEdgeTypePreference.Drop_Shadow,
) : UserPreferences
