package com.flixclusive.core.ui.player.util

internal fun String?.isDdosProtection(): Boolean {
    if (this == null) return true

    return contains("ddos", true)
    || contains("cloudfare", true)
}