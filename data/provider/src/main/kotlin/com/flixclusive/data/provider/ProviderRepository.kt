package com.flixclusive.data.provider

import androidx.compose.runtime.snapshots.SnapshotStateList
import com.flixclusive.model.datastore.AppSettings
import com.flixclusive.provider.base.ProviderData

interface ProviderRepository {
    val providers: SnapshotStateList<ProviderData>

    fun initialize()

    suspend fun swap(appSettings: AppSettings, fromIndex: Int, toIndex: Int)

    suspend fun toggleUsage(appSettings: AppSettings, index: Int)
}