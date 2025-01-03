package com.flixclusive.core.datastore.di

import android.content.Context
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.DefaultUserSessionDataStore
import com.flixclusive.core.datastore.UserSessionDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DataStoreModule {
    @Provides
    @Singleton
    fun providesDataStoreManager(
        context: Context,
        userSessionDataStore: UserSessionDataStore
    ): DataStoreManager = DataStoreManager(
        context = context,
        userSessionDataStore = userSessionDataStore
    )

    @Provides
    @Singleton
    fun providesUserSessionDataStore(
        context: Context
    ): UserSessionDataStore = DefaultUserSessionDataStore(context = context)
}