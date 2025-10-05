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
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DomainDownloadsModule {
    @Binds
    abstract fun bindDownloadServiceController(impl: DownloadServiceControllerImpl): DownloadServiceController

    @Binds
    abstract fun bindDownloadFileUseCase(impl: DownloadFileUseCaseImpl): DownloadFileUseCase

    @Binds
    abstract fun bindCancelDownloadUseCase(impl: CancelDownloadUseCaseImpl): CancelDownloadUseCase
}
