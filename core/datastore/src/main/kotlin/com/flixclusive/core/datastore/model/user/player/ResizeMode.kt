package com.flixclusive.core.datastore.model.user.player

import com.flixclusive.core.datastore.R

enum class ResizeMode(val mode: Int) {
    Fit(0),
    Fill(3),
    Zoom(4);

    fun getStringResId(): Int {
        return when(this) {
            Fill -> R.string.resize_mode_stretch
            Zoom -> R.string.resize_mode_center_crop
            else -> R.string.resize_mode_fit
        }
    }
}
