package com.flixclusive.di

import android.app.Application
import com.flixclusive.data.preferences.VideoDataServerPreferencesImpl
import com.flixclusive.domain.preferences.VideoDataServerPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppPreferencesModule {

    @Provides
    @Singleton
    fun provideVideoDataServerPreferences(
        application: Application
    ): VideoDataServerPreferences = VideoDataServerPreferencesImpl(application)
}