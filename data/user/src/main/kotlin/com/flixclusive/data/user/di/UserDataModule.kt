package com.flixclusive.data.user.di

import com.flixclusive.data.user.DefaultUserRepository
import com.flixclusive.data.user.UserRepository
import com.flixclusive.data.user.local.LocalUserDataSource
import com.flixclusive.data.user.local.UserDataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class UserDataModule {
    @Singleton
    @Binds
    abstract fun bindsUserRepository(
        userRepository: DefaultUserRepository,
    ): UserRepository

    @Singleton
    @Binds
    abstract fun bindsLocalUserDataSource(
        localDataSource: LocalUserDataSource,
    ): UserDataSource
}