package com.flixclusive.model.datastore.player

import com.flixclusive.core.util.common.ui.UiText
import com.flixclusive.core.util.R as UtilR

enum class DecoderPriority {
    PREFER_DEVICE,
    PREFER_APP,
    DEVICE_ONLY;

    fun toUiText(): UiText {
        return when (this) {
            PREFER_DEVICE -> UiText.StringResource(UtilR.string.decoder_prefer_device)
            PREFER_APP -> UiText.StringResource(UtilR.string.decoder_prefer_app)
            DEVICE_ONLY -> UiText.StringResource(UtilR.string.decoder_device_only)
        }
    }
}