package com.flixclusive.core.datastore.di

import android.content.Context
import androidx.datastore.core.DataStore
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.DataStoreManagerImpl
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.core.datastore.UserSessionDataStoreImpl
import com.flixclusive.core.datastore.model.system.SystemPreferences
import com.flixclusive.core.datastore.systemPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DataStoreModule {
    @Provides
    @Singleton
    fun providesDataStoreManager(
        @ApplicationContext context: Context,
        userSessionDataStore: UserSessionDataStore,
        systemPreferences: DataStore<SystemPreferences>,
        appDispatchers: AppDispatchers,
    ): DataStoreManager = DataStoreManagerImpl(
        context = context,
        userSessionDataStore = userSessionDataStore,
        systemPreferences = systemPreferences,
        appDispatchers = appDispatchers
    )

    @Provides
    @Singleton
    fun providesSystemPreferencesDataStore(
        @ApplicationContext context: Context
    ) = context.systemPreferences

    @Provides
    @Singleton
    fun providesUserSessionDataStore(
        @ApplicationContext context: Context,
    ): UserSessionDataStore = UserSessionDataStoreImpl(context = context)
}
