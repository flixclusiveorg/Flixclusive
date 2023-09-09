package com.flixclusive.di

import android.app.Application
import com.flixclusive.data.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(
        application: Application,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ) : AppDatabase {
        return AppDatabase.getInstance(application, ioDispatcher)
    }
}