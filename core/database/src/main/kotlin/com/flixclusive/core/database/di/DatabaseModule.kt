package com.flixclusive.core.database.di

import android.app.Application
import com.flixclusive.core.database.AppDatabase
import com.flixclusive.core.util.common.dispatcher.di.ApplicationScope
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(
        application: Application,
        @ApplicationScope scope: CoroutineScope
    ) : AppDatabase = AppDatabase.getInstance(application, scope)
}