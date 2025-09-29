package com.flixclusive.data.app.updates.di

import com.flixclusive.data.app.updates.repository.AppUpdatesRepository
import com.flixclusive.data.app.updates.repository.impl.GithubUpdatesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataAppUpdatesModule {
    @Binds
    abstract fun bindGithubUpdatesRepository(impl: GithubUpdatesRepository): AppUpdatesRepository
}
