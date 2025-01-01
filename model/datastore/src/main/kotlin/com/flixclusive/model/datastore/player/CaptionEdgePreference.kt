package com.flixclusive.model.datastore.player

import android.annotation.SuppressLint
import android.graphics.Color
import com.flixclusive.core.locale.UiText
import com.flixclusive.core.locale.R as LocaleR

@Suppress("EnumEntryName")
@SuppressLint("UnsafeOptInUsageError")
enum class CaptionEdgeTypePreference(
    val type: Int,
    val color: Int = Color.BLACK
) {
    Drop_Shadow(2),
    Outline(1);

    fun toUiText(): UiText {
        return when (this) {
            Drop_Shadow -> UiText.StringResource(LocaleR.string.drop_shadow)
            Outline -> UiText.StringResource(LocaleR.string.outline)
        }
    }
}