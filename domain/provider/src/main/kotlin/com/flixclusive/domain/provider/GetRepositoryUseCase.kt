package com.flixclusive.domain.provider

import com.flixclusive.core.datastore.AppSettingsManager
import com.flixclusive.core.util.common.dispatcher.AppDispatchers
import com.flixclusive.core.util.common.dispatcher.Dispatcher
import com.flixclusive.core.util.common.resource.Resource
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.domain.provider.util.extractGithubInfoFromLink
import com.flixclusive.domain.provider.util.isProviderBranchValid
import com.flixclusive.gradle.entities.Repository
import com.flixclusive.gradle.entities.Repository.Companion.toValidRepositoryLink
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import javax.inject.Inject
import com.flixclusive.core.util.R as UtilR

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
 * @param ioDispatcher The dispatcher used to run the network call on a background thread.
 */
class GetRepositoryUseCase @Inject constructor(
    private val client: OkHttpClient,
    private val appSettingsManager: AppSettingsManager,
    @Dispatcher(AppDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(url: String): Resource<Repository> {
        return withContext(ioDispatcher) {
            safeCall {
                val repositories = appSettingsManager.providerSettings.data.map { it.repositories }.first()
                val (username, repositoryName) = extractGithubInfoFromLink(url) ?: (null to null)

                val isAlreadyAdded = repositories.any { it.owner.equals(username, true) && it.name == repositoryName }
                if (isAlreadyAdded) {
                    return@withContext Resource.Failure(UtilR.string.already_added_repo_error)
                }

                val repository = url.toValidRepositoryLink()
                val providerBranch = repository.getRawLink(
                    filename = "updater.json",
                    branch = "builds"
                )

                if (!client.isProviderBranchValid(providerBranch)) {
                    return@withContext Resource.Failure(UtilR.string.invalid_repository)
                }

                return@withContext Resource.Success(repository)
            } ?: Resource.Failure(UtilR.string.invalid_repo_link)
        }
    }


}