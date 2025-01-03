package com.flixclusive.core.datastore.migration.serializer

import com.flixclusive.core.datastore.migration.model.OldAppSettingsProvider
import com.flixclusive.core.datastore.util.BaseSettingsSerializer

internal object AppSettingsProviderSerializer : BaseSettingsSerializer<OldAppSettingsProvider>(
    serializer = OldAppSettingsProvider.serializer()
) {
    override val defaultValue: OldAppSettingsProvider
        get() = OldAppSettingsProvider()
}