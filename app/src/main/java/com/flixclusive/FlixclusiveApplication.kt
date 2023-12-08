package com.flixclusive

import android.app.Application
import android.content.Context
import androidx.datastore.dataStore
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.flixclusive.domain.config.ConfigurationProvider
import com.flixclusive.domain.preferences.AppSettingsManager
import com.flixclusive.domain.preferences.AppSettingsSerializer
import com.flixclusive.presentation.mobile.screens.crash.GlobalCrashHandler
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient
import javax.inject.Inject


val Context.appSettingsDataStore by dataStore("app-preferences.json", AppSettingsSerializer)

@HiltAndroidApp
class FlixclusiveApplication : Application(), ImageLoaderFactory {
    @Inject
    lateinit var configurationProvider: ConfigurationProvider
    @Inject
    lateinit var appSettingsManager: AppSettingsManager
    @Inject
    lateinit var client: OkHttpClient

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .okHttpClient(client)
            .build()
    }

    override fun onCreate() {
        super.onCreate()

        GlobalCrashHandler.initialize(applicationContext)

        configurationProvider.initialize()
        appSettingsManager.initialize()
    }
}