package com.flixclusive.data.user.di

import com.flixclusive.data.user.DefaultUserRepository
import com.flixclusive.data.user.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class UserDataModule {
    @Binds
    internal abstract fun bindsUserRepository(
        userRepository: DefaultUserRepository,
    ): UserRepository

}