package com.flixclusive.domain.usecase

import androidx.compose.runtime.snapshots.SnapshotStateList
import com.flixclusive.domain.model.provider.SourceProviderDetails

interface ModifyProvidersUseCase {
    val availableProviders: SnapshotStateList<SourceProviderDetails>

    fun swap(fromIndex: Int, toIndex: Int)

    fun toggleUsage(index: Int)
}