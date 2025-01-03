package com.flixclusive.core.datastore.migration.serializer

import com.flixclusive.core.datastore.migration.model.OldAppSettings
import com.flixclusive.core.datastore.util.BaseSettingsSerializer

internal object AppSettingsSerializer : BaseSettingsSerializer<OldAppSettings>(
    serializer = OldAppSettings.serializer()
) {
    override val defaultValue = OldAppSettings()
}