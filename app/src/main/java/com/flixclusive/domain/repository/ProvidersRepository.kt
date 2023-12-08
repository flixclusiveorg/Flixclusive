package com.flixclusive.domain.repository

import androidx.compose.runtime.snapshots.SnapshotStateList
import com.flixclusive.domain.model.provider.SourceProviderDetails
import com.flixclusive.domain.preferences.AppSettings

interface ProvidersRepository {
    val providers: SnapshotStateList<SourceProviderDetails>

    fun populate(
        name: String,
        isIgnored: Boolean,
        isMaintenance: Boolean
    )

    suspend fun swap(appSettings: AppSettings, fromIndex: Int, toIndex: Int)

    suspend fun toggleUsage(appSettings: AppSettings, index: Int)
}