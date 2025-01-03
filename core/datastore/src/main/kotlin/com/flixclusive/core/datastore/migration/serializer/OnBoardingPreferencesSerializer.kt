package com.flixclusive.core.datastore.migration.serializer

import com.flixclusive.core.datastore.migration.model.OldOnBoardingPreferences
import com.flixclusive.core.datastore.util.BaseSettingsSerializer

internal object OnBoardingPreferencesSerializer : BaseSettingsSerializer<OldOnBoardingPreferences>(
    serializer = OldOnBoardingPreferences.serializer()
) {
    override val defaultValue: OldOnBoardingPreferences
        get() = OldOnBoardingPreferences()
}