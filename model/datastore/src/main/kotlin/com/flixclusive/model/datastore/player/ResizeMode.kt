package com.flixclusive.model.datastore.player

enum class ResizeMode(val mode: Int) {
    Fit(0),
    Fill(3),
    Zoom(4);

    override fun toString(): String {
        return when(this) {
            Fill -> "Stretch"
            Zoom -> "Center Crop"
            else -> super.toString()
        }
    }
}