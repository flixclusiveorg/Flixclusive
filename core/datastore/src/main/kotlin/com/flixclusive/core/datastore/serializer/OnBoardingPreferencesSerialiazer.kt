package com.flixclusive.core.datastore.serializer

import com.flixclusive.model.datastore.OnBoardingPreferences

internal object OnBoardingPreferencesSerializer : BaseSettingsSerializer<OnBoardingPreferences>(
    serializer = OnBoardingPreferences.serializer()
) {
    override val defaultValue: OnBoardingPreferences
        get() = OnBoardingPreferences()
}