package com.flixclusive.model.datastore

import com.flixclusive.core.util.common.GithubConstant.GITHUB_BUILT_IN_PROVIDERS_REPOSITORY
import com.flixclusive.model.provider.Repository
import com.flixclusive.model.provider.Repository.Companion.toValidRepositoryLink
import com.flixclusive.model.datastore.provider.ProviderPreference
import kotlinx.serialization.Serializable


/**
 *
 * A sub data class for provider settings of
 * the main [AppSettings] data class.
 * */
@Serializable
data class AppSettingsProvider(
    val warnOnInstall: Boolean = true,
    val isUsingAutoUpdateProviderFeature: Boolean = true,
    val repositories: List<Repository> = listOf(GITHUB_BUILT_IN_PROVIDERS_REPOSITORY.toValidRepositoryLink()),
    val providers: List<ProviderPreference> = emptyList(),
)