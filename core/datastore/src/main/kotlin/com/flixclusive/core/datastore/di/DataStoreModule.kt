package com.flixclusive.core.datastore.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import com.flixclusive.core.datastore.AppSettingsSerializer
import com.flixclusive.model.datastore.AppSettings
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    private val Context.appSettings by dataStore("app-preferences.json", AppSettingsSerializer)

    @Provides
    @Singleton
    internal fun providesUserPreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<AppSettings> = context.appSettings
}