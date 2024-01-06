package com.flixclusive.core.datastore

import androidx.datastore.core.DataStore
import com.flixclusive.model.datastore.AppSettings
import com.flixclusive.core.util.common.network.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

class AppSettingsManager @Inject constructor(
    val appSettings: DataStore<AppSettings>,
    @ApplicationScope private val scope: CoroutineScope
) {
    var localAppSettings: AppSettings = AppSettings()
        private set

    fun initialize() {
        scope.launch {
            localAppSettings = appSettings.data.first()
        }
    }

    suspend fun updateData(newValue: AppSettings) {
        localAppSettings = newValue
        appSettings.updateData { newValue }
    }
}