package com.flixclusive.domain.provider.usecase.manage

import com.flixclusive.core.database.entity.provider.InstalledProvider

interface UnloadProviderUseCase {
    suspend operator fun invoke(
        provider: InstalledProvider,
        uninstall: Boolean = true
    )
}
