package com.flixclusive.core.util.common.dispatcher.di

import com.flixclusive.core.util.common.dispatcher.AppDispatchers
import com.flixclusive.core.util.common.dispatcher.Dispatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object DispatchersModule {
    @Provides
    @Dispatcher(AppDispatchers.IO)
    fun providesIODispatcher()
        = AppDispatchers.IO.dispatcher

    @Provides
    @Dispatcher(AppDispatchers.Default)
    fun providesDefaultDispatcher()
        = AppDispatchers.Default.dispatcher

    @Provides
    @Dispatcher(AppDispatchers.Main)
    fun providesMainDispatcher()
        = AppDispatchers.Main.dispatcher
}