package com.flixclusive.core.datastore.serializer.system

import com.flixclusive.core.datastore.model.system.SystemPreferences
import com.flixclusive.core.datastore.serializer.BaseSettingsSerializer

internal object SystemPreferencesSerializer
    : BaseSettingsSerializer<SystemPreferences>(SystemPreferences.serializer()) {
    override val defaultValue = SystemPreferences()
}
