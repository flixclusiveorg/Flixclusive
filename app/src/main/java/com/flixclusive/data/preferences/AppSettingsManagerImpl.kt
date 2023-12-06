package com.flixclusive.data.preferences

import androidx.datastore.core.DataStore
import com.flixclusive.di.IoDispatcher
import com.flixclusive.domain.preferences.AppSettings
import com.flixclusive.domain.preferences.AppSettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

class AppSettingsManagerImpl @Inject constructor(
    override val appSettings: DataStore<AppSettings>,
    @IoDispatcher private val ioScope: CoroutineScope
): AppSettingsManager {
    override var localAppSettings: AppSettings = AppSettings()

    override fun initialize() {
        ioScope.launch {
            localAppSettings = appSettings.data.first()
        }
    }

    override suspend fun updateData(newValue: AppSettings) {
        localAppSettings = newValue
        appSettings.updateData { newValue }
    }
}