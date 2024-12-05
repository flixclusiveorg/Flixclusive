package com.flixclusive.model.datastore.player

import com.flixclusive.core.locale.UiText
import com.flixclusive.core.locale.R as LocaleR

enum class ResizeMode(val mode: Int) {
    Fit(0),
    Fill(3),
    Zoom(4);

    fun toUiText(): UiText {
        return when(this) {
            Fill -> UiText.StringResource(LocaleR.string.resize_mode_stretch)
            Zoom -> UiText.StringResource(LocaleR.string.resize_mode_center_crop)
            else -> UiText.StringResource(LocaleR.string.resize_mode_fit)
        }
    }
}