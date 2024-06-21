package com.flixclusive.domain.provider.di

import com.flixclusive.data.provider.di.TestProviderDataModule.getMockProviderApiMap
import com.flixclusive.domain.provider.SourceLinksProviderUseCase
import io.mockk.every
import io.mockk.mockk

object TestProviderDomainModule {
    fun getMockSourceLinksProviderUseCase(): SourceLinksProviderUseCase {
        return mockk {
            every { providerApis } returns getMockProviderApiMap().values.toList()
        }
    }
}