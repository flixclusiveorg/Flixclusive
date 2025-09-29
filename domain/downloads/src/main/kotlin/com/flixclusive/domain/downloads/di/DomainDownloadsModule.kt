package com.flixclusive.domain.downloads.di

import com.flixclusive.domain.downloads.controller.DownloadServiceController
import com.flixclusive.domain.downloads.controller.impl.DownloadServiceControllerImpl
import com.flixclusive.domain.downloads.usecase.CancelDownloadUseCase
import com.flixclusive.domain.downloads.usecase.DownloadFileUseCase
import com.flixclusive.domain.downloads.usecase.impl.CancelDownloadUseCaseImpl
import com.flixclusive.domain.downloads.usecase.impl.DownloadFileUseCaseImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
internal abstract class DomainDownloadsModule {
    @Binds
    @ViewModelScoped
    abstract fun bindDownloadServiceController(impl: DownloadServiceControllerImpl): DownloadServiceController

    @Binds
    @ViewModelScoped
    abstract fun bindDownloadFileUseCase(impl: DownloadFileUseCaseImpl): DownloadFileUseCase

    @Binds
    @ViewModelScoped
    abstract fun bindCancelDownloadUseCase(impl: CancelDownloadUseCaseImpl): CancelDownloadUseCase
}
