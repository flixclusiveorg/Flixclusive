package com.flixclusive.data.database.di

import com.flixclusive.data.database.session.UserSessionManager
import com.flixclusive.data.database.session.impl.UserSessionManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class UserSessionModule {
    @Singleton
    @Binds
    abstract fun bindsUserSessionManager(
        userSessionManager: UserSessionManagerImpl,
    ): UserSessionManager
}
