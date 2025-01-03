package com.flixclusive.model.datastore.user

import android.graphics.Color
import com.flixclusive.model.datastore.user.player.CaptionEdgeTypePreference
import com.flixclusive.model.datastore.user.player.CaptionStylePreference
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