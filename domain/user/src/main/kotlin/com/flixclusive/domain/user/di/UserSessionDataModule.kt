package com.flixclusive.domain.user.di

import com.flixclusive.domain.user.DefaultUserSessionManager
import com.flixclusive.domain.user.UserSessionManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class UserSessionDataModule {
    @Singleton
    @Binds
    abstract fun bindsUserSessionManager(
        userSessionManager: DefaultUserSessionManager,
    ): UserSessionManager
}