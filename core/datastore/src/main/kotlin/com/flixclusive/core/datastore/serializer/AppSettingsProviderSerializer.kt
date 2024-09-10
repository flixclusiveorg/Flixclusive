package com.flixclusive.core.datastore.serializer

import com.flixclusive.model.datastore.AppSettingsProvider

internal object AppSettingsProviderSerializer : BaseSettingsSerializer<AppSettingsProvider>(
    serializer = AppSettingsProvider.serializer()
) {
    override val defaultValue: AppSettingsProvider
        get() = AppSettingsProvider()
}