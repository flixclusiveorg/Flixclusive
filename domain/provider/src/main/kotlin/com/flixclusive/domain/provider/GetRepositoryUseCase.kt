package com.flixclusive.domain.provider

import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.util.coroutines.AppDispatchers.Companion.withIOContext
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.domain.provider.util.isUrlOnline
import com.flixclusive.domain.provider.util.toGithubUrl
import com.flixclusive.model.datastore.user.ProviderPreferences
import com.flixclusive.model.datastore.user.UserPreferences
import com.flixclusive.model.provider.Repository
import com.flixclusive.model.provider.Repository.Companion.toValidRepositoryLink
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import javax.inject.Inject
import com.flixclusive.core.locale.R as LocaleR

/**
 * Use case to validate and retrieve a [Repository] from a given URL.
 *
 * This use case takes a URL as input, validates it to be a valid repository link format,
 * and then attempts to fetch the "updater.json" file from the "builds" branch of the repository.
 * If the URL is invalid or the "updater.json" file cannot be retrieved successfully,
 * a [Resource.Failure] is returned. Otherwise, a [Resource.Success] containing the validated
 * repository object is returned.
 *
 * @param client The OkHttpClient instance used to make network requests.
 */
class GetRepositoryUseCase
    @Inject
    constructor(
        private val client: OkHttpClient,
        private val dataStoreManager: DataStoreManager,
    ) {
        suspend operator fun invoke(url: String): Resource<Repository> {
            return withIOContext {
                safeCall {
                    val repositories =
                        dataStoreManager
                            .getUserPrefs<ProviderPreferences>(UserPreferences.PROVIDER_PREFS_KEY)
                            .first()
                            .repositories

                    val repositoryUrl = url.toGithubUrl()
                    if (repositoryUrl == null) {
                        return@withIOContext Resource.Failure(LocaleR.string.invalid_repository_url)
                    }

                    val repository = repositoryUrl.toValidRepositoryLink()
                    val isAlreadyAdded =
                        repositories.any {
                            it.owner.equals(repository.owner, true) &&
                                it.name == repository.name
                        }

                    if (isAlreadyAdded) {
                        return@withIOContext Resource.Failure(LocaleR.string.already_added_repo_error)
                    }

                    val providerBranch =
                        repository.getRawLink(
                            filename = UPDATER_FILE,
                            branch = "builds",
                        )

                    if (!client.isUrlOnline(providerBranch)) {
                        return@withIOContext Resource.Failure(LocaleR.string.invalid_repository)
                    }

                    return@withIOContext Resource.Success(repository)
                } ?: Resource.Failure(LocaleR.string.invalid_repo_link)
            }
        }
    }
