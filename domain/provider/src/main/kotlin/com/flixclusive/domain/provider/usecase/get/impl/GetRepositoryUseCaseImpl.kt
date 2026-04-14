package com.flixclusive.domain.provider.usecase.get.impl

import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.provider.ProviderConstants
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.core.network.util.Resource
import com.flixclusive.data.provider.repository.InstalledRepoRepository
import com.flixclusive.domain.provider.R
import com.flixclusive.domain.provider.usecase.get.GetRepositoryUseCase
import com.flixclusive.domain.provider.util.extensions.isUrlOnline
import com.flixclusive.domain.provider.util.toGithubUrl
import com.flixclusive.model.provider.Repository
import com.flixclusive.model.provider.Repository.Companion.toValidRepositoryLink
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import javax.inject.Inject

internal class GetRepositoryUseCaseImpl @Inject constructor(
    private val client: OkHttpClient,
    private val userSessionDataStore: UserSessionDataStore,
    private val installedRepoRepository: InstalledRepoRepository,
    private val appDispatchers: AppDispatchers,
) : GetRepositoryUseCase {
    override suspend operator fun invoke(url: String): Resource<Repository> {
        return withContext(appDispatchers.io) {
            try {
                val repositoryUrl = url.toGithubUrl()
                    ?: return@withContext Resource.Failure(R.string.invalid_repository_url)

                val repository = repositoryUrl.toValidRepositoryLink()
                if (isRepositoryAlreadyAdded(repository)) {
                    return@withContext Resource.Failure(R.string.already_added_repo_error)
                }

                val providerBranchUrl = repository.getRawLink(
                    filename = ProviderConstants.UPDATER_JSON_FILE,
                    branch = "builds",
                )

                if (!client.isUrlOnline(providerBranchUrl)) {
                    return@withContext Resource.Failure(R.string.invalid_repository)
                }

                Resource.Success(repository)
            } catch (e: Exception) {
                Resource.Failure(e)
            }
        }
    }

    private suspend fun isRepositoryAlreadyAdded(repository: Repository): Boolean {
        val userId = userSessionDataStore.currentUserId.filterNotNull().first()

        return installedRepoRepository.isInstalled(repository.url, userId)
    }
}
