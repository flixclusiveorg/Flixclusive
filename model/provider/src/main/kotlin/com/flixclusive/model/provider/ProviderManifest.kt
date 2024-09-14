package com.flixclusive.model.provider

import kotlinx.serialization.Serializable

/**
 * Represents the manifest information of a provider.
 *
 * @property providerClassName The fully qualified class name of the provider.
 * @property name The name of the provider.
 * @property versionName The version name of the provider.
 * @property versionCode The version code of the provider.
 * @property requiresResources Indicates whether the provider requires resources from the main application/apk.
 */
@Serializable
data class ProviderManifest(
    val providerClassName: String,
    val name: String,
    val versionName: String,
    val versionCode: Long,
    val requiresResources: Boolean,
    val updateUrl: String?,
)
