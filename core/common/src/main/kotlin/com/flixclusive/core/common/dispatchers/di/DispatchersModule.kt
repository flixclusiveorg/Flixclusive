package com.flixclusive.core.common.dispatchers.di

import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.dispatchers.AppDispatchersImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DispatchersModule {

    @Binds
    internal abstract fun bindAppDispatchers(
        appDispatchersImpl: AppDispatchersImpl
    ): AppDispatchers
}
