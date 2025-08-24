package com.flixclusive.core.datastore.model.user.player

import com.flixclusive.core.datastore.R

enum class ResizeMode {
    Fit,
    Crop,
    None,
    Inside,
    Fill,
    FillHeight,
    FillWidth;

    fun getStringId(): Int {
        return when(this) {
            Fit -> R.string.resize_mode_fit
            Crop -> R.string.resize_mode_crop
            None -> R.string.resize_mode_none
            Inside -> R.string.resize_mode_inside
            Fill -> R.string.resize_mode_fill
            FillHeight -> R.string.resize_mode_fill_height
            FillWidth -> R.string.resize_mode_fill_width
        }
    }
}
