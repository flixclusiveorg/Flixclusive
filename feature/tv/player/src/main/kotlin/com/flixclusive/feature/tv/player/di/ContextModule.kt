package com.flixclusive.feature.tv.player.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.assisted.AssistedInject
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 *
 * Because @[AssistedInject] won't work without this
 * being provided as a singleton.
 * */
@Module
@InstallIn(SingletonComponent::class)
internal object ContextModule {
    @Singleton
    @Provides
    fun provideContext(@ApplicationContext context: Context) = context
}