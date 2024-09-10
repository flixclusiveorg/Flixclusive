package com.flixclusive.core.datastore.serializer

import com.flixclusive.model.datastore.AppSettings

internal object AppSettingsSerializer : BaseSettingsSerializer<AppSettings>(
    serializer = AppSettings.serializer()
) {
    override val defaultValue: AppSettings
        get() = AppSettings()
}