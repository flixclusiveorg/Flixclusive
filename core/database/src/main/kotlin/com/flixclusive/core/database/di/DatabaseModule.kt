package com.flixclusive.core.database.di

import android.app.Application
import com.flixclusive.core.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(
        application: Application
    ) : AppDatabase = AppDatabase.getInstance(application)
}