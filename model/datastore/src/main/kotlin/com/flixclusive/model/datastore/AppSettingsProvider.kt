package com.flixclusive.model.datastore

import com.flixclusive.core.util.common.configuration.GITHUB_BUILT_IN_PROVIDERS_REPOSITORY
import com.flixclusive.gradle.entities.Repository
import com.flixclusive.gradle.entities.Repository.Companion.toValidRepositoryLink
import com.flixclusive.model.datastore.provider.ProviderPreference
import kotlinx.serialization.Serializable


/**
 *
 * A sub data class for provider settings of
 * the main [AppSettings] data class.
 * */
@Serializable
data class AppSettingsProvider(
    val repositories: List<Repository> = listOf(GITHUB_BUILT_IN_PROVIDERS_REPOSITORY.toValidRepositoryLink()),
    val providers: List<ProviderPreference> = emptyList(),
)