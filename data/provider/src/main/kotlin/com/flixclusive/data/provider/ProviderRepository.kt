package com.flixclusive.data.provider

import androidx.compose.runtime.snapshots.SnapshotStateList
import com.flixclusive.model.datastore.AppSettings
import com.flixclusive.model.provider.ProviderWrapper

interface ProviderRepository {
    val providers: SnapshotStateList<ProviderWrapper>

    fun populate(
        name: String,
        isIgnored: Boolean,
        isMaintenance: Boolean
    )

    suspend fun swap(appSettings: AppSettings, fromIndex: Int, toIndex: Int)

    suspend fun toggleUsage(appSettings: AppSettings, index: Int)
}