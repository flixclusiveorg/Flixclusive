package com.flixclusive.domain.preferences

import androidx.datastore.core.DataStore

interface AppSettingsManager {
    val appSettings: DataStore<AppSettings>
    var localAppSettings: AppSettings

    fun initialize()

    suspend fun updateData(newValue: AppSettings)
}