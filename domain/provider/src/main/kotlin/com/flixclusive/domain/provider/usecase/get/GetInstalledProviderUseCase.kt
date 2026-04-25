package com.flixclusive.domain.provider.usecase.get

import com.flixclusive.core.database.entity.provider.InstalledProvider

interface GetInstalledProviderUseCase {
    suspend operator fun invoke(id: String): InstalledProvider?
}
