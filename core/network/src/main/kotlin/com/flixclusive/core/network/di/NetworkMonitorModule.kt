package com.flixclusive.core.network.di

import com.flixclusive.core.network.monitor.NetworkMonitor
import com.flixclusive.core.network.monitor.NetworkMonitorImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class NetworkMonitorModule {
    @Singleton
    @Binds
    internal abstract fun bindsNetworkMonitor(networkMonitorImpl: NetworkMonitorImpl): NetworkMonitor
}
