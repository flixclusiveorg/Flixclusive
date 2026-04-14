package com.flixclusive.domain.provider.usecase.manage

import com.flixclusive.core.database.entity.provider.InstalledProvider
import kotlinx.coroutines.flow.Flow

/**
 * Use case for loading provider from the local storage.
 *
 */
interface LoadProviderUseCase {
    operator fun invoke(installedProvider: InstalledProvider): Flow<ProviderResult>
}
