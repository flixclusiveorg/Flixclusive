package com.flixclusive.core.datastore.migration.model

import com.flixclusive.model.datastore.user.ProviderOrderEntity
import com.flixclusive.model.provider.Repository
import kotlinx.serialization.Serializable


/**
 *
 * A sub data class for provider settings of
 * the main [OldAppSettings] data class.
 * */
@Serializable
internal data class OldAppSettingsProvider(
    val warnOnInstall: Boolean = true,
    val isUsingAutoUpdateProviderFeature: Boolean = true,
    val repositories: List<Repository> = listOf(),
    val providers: List<ProviderOrderEntity> = emptyList(),
)