package com.flixclusive.domain.provider.di

import androidx.compose.runtime.snapshotFlow
import com.flixclusive.data.provider.di.TestProviderDataModule.getMockProviderApiMap
import com.flixclusive.domain.provider.GetMediaLinksUseCase
import io.mockk.every
import io.mockk.mockk

object TestProviderDomainModule {
    fun getMockSourceLinksProviderUseCase(): GetMediaLinksUseCase {
        return mockk {
            every { providerApis } returns snapshotFlow {
                getMockProviderApiMap().values.toList()
            }
        }
    }
}