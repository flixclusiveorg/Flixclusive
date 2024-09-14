package com.flixclusive.model.datastore.player

import com.flixclusive.core.locale.UiText
import com.flixclusive.core.locale.R as LocaleR

enum class DecoderPriority {
    PREFER_DEVICE,
    PREFER_APP,
    DEVICE_ONLY;

    fun toUiText(): UiText {
        return when (this) {
            PREFER_DEVICE -> UiText.StringResource(LocaleR.string.decoder_prefer_device)
            PREFER_APP -> UiText.StringResource(LocaleR.string.decoder_prefer_app)
            DEVICE_ONLY -> UiText.StringResource(LocaleR.string.decoder_device_only)
        }
    }
}