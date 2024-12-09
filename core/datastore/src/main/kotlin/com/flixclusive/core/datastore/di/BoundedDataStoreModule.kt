package com.flixclusive.core.datastore.di

import com.flixclusive.core.datastore.DefaultUserSessionDataStore
import com.flixclusive.core.datastore.UserSessionDataStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class BoundedDataStoreModule {
    @Singleton
    @Binds
    abstract fun providesUserSessionDataStore(
        userDataStore: DefaultUserSessionDataStore
    ): UserSessionDataStore
}