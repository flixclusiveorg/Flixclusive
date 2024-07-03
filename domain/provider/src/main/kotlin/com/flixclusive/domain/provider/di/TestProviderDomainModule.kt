package com.flixclusive.domain.provider.di

import androidx.compose.runtime.snapshotFlow
import com.flixclusive.data.provider.di.TestProviderDataModule.getMockProviderApiMap
import com.flixclusive.domain.provider.SourceLinksProviderUseCase
import io.mockk.every
import io.mockk.mockk

object TestProviderDomainModule {
    fun getMockSourceLinksProviderUseCase(): SourceLinksProviderUseCase {
        return mockk {
            every { providerApis } returns snapshotFlow {
                getMockProviderApiMap().values.toList()
            }
        }
    }
}