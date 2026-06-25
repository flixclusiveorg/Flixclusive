package com.flixclusive.feature.mobile.library.common.model

import androidx.compose.runtime.Stable
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.model.provider.ProviderStatus

@Stable
data class TrackerProvider(
    val isTrackerEnabled: Boolean,
    val isAuthenticated: Boolean,
    val metadata: ProviderMetadata,
) {
    val id: String get() = metadata.id
    val name: String get() = metadata.name

    val iconUrl: String? get() = metadata.iconUrl
    val versionName: String get() = metadata.versionName
    val versionCode: Long get() = metadata.versionCode
    val status: ProviderStatus get() = metadata.status
}
