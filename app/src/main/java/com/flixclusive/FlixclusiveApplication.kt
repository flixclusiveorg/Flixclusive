package com.flixclusive

import android.app.Application
import android.content.Context
import androidx.datastore.dataStore
import com.flixclusive.domain.config.ConfigurationProvider
import com.flixclusive.domain.preferences.AppSettingsSerializer
import com.flixclusive.domain.repository.UserRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject


val Context.appSettingsDataStore by dataStore("app-preferences.json", AppSettingsSerializer)

@HiltAndroidApp
class FlixclusiveApplication : Application() {
    @Inject
    lateinit var configurationProvider: ConfigurationProvider

    @Inject
    lateinit var userRepository: UserRepository

    override fun onCreate() {
        super.onCreate()
        configurationProvider.initialize()
    }
}