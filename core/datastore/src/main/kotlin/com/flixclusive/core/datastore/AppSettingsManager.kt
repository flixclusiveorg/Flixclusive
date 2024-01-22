package com.flixclusive.core.datastore

import androidx.datastore.core.DataStore
import com.flixclusive.core.util.common.dispatcher.di.ApplicationScope
import com.flixclusive.model.datastore.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

class AppSettingsManager @Inject constructor(
    val appSettings: DataStore<AppSettings>,
    @ApplicationScope private val scope: CoroutineScope
) {
    /**
     *
     * Used for initial values.
     * */
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