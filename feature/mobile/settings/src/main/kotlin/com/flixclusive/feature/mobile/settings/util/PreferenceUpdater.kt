package com.flixclusive.feature.mobile.settings.util

internal interface PreferenceUpdater<T> {
    fun onUpdatePreferences(transform: suspend (t: T) -> T)
}
