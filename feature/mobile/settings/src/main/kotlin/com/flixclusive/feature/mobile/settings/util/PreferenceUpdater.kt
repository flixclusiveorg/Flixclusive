package com.flixclusive.feature.mobile.settings.util

internal interface PreferenceUpdater<T> {
    /**
     * Updates the user preferences by applying the provided transformation function.
     *
     * @param transform A suspend function that takes the current preferences and returns the updated preferences.
     *
     * @return A boolean indicating whether the update was successful.
     * */
    suspend fun onUpdatePreferences(transform: suspend (t: T) -> T): Boolean
}
