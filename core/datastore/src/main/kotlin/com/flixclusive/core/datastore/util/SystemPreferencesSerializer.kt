package com.flixclusive.core.datastore.util

import com.flixclusive.model.datastore.system.SystemPreferences

internal object SystemPreferencesSerializer
    : BaseSettingsSerializer<SystemPreferences>(SystemPreferences.serializer()) {
    override val defaultValue = SystemPreferences()
}