package com.flixclusive.core.datastore.model.user.player

import com.flixclusive.core.datastore.R

enum class DecoderPriority {
    PREFER_DEVICE,
    PREFER_APP,
    DEVICE_ONLY;

    fun getStringResId(): Int {
        return when (this) {
            PREFER_DEVICE -> R.string.decoder_prefer_device
            PREFER_APP -> R.string.decoder_prefer_app
            DEVICE_ONLY -> R.string.decoder_device_only
        }
    }
}
