package com.flixclusive.core.datastore

import androidx.datastore.core.DataStore
import com.flixclusive.core.util.common.dispatcher.di.ApplicationScope
import com.flixclusive.model.datastore.AppSettings
import com.flixclusive.model.datastore.AppSettingsProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

class AppSettingsManager @Inject constructor(
    val appSettings: DataStore<AppSettings>,
    val providerSettings: DataStore<AppSettingsProvider>,
    @ApplicationScope private val scope: CoroutineScope
) {
    /**
     *
     * Used for initial [AppSettings] values.
     * */
    var localAppSettings: AppSettings = AppSettings()
        private set

    /**
     *
     * Used for initial [AppSettingsProvider] values.
     * */
    var localProviderSettings: AppSettingsProvider = AppSettingsProvider()
        private set

    fun initialize() {
        scope.launch {
            localAppSettings = appSettings.data.first()
            localProviderSettings = providerSettings.data.first()
        }
    }

    suspend fun updateSettings(newValue: AppSettings) {
        updateSettings { newValue }
    }

    suspend fun updateSettings(transform: suspend (t: AppSettings) -> AppSettings) {
        appSettings.updateData {
            val newSettings = transform(it)

            localAppSettings = newSettings
            newSettings
        }
    }

    suspend fun updateProviderSettings(transform: suspend (t: AppSettingsProvider) -> AppSettingsProvider) {
        providerSettings.updateData {
            val newSettings = transform(it)

            localProviderSettings = newSettings
            newSettings
        }
    }
}