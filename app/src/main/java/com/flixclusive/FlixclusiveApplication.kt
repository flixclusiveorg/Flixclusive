package com.flixclusive

import android.app.Application
import com.flixclusive.domain.firebase.ConfigurationProvider
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class FlixclusiveApplication : Application() {
    @Inject
    lateinit var configurationProvider: ConfigurationProvider

    override fun onCreate() {
        super.onCreate()
        configurationProvider.initialize()
    }
}