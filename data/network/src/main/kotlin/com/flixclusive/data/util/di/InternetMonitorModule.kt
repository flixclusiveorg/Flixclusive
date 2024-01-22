package com.flixclusive.data.util.di

import com.flixclusive.data.util.InternetMonitor
import com.flixclusive.data.util.InternetMonitorManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class InternetMonitorModule {
    @Singleton
    @Binds
    internal abstract fun bindsInternetMonitor(
        internetMonitorManager: InternetMonitorManager
    ): InternetMonitor
}