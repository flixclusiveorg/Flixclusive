package com.flixclusive.domain.provider

import com.flixclusive.core.util.common.dispatcher.AppDispatchers
import com.flixclusive.core.util.common.dispatcher.Dispatcher
import com.flixclusive.core.util.common.resource.Resource
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.core.util.network.request
import com.flixclusive.gradle.entities.Repository
import com.flixclusive.gradle.entities.Repository.Companion.toValidRepositoryLink
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import javax.inject.Inject
import com.flixclusive.core.util.R as UtilR

class GetRepositoryUseCase @Inject constructor(
    private val client: OkHttpClient,
    @Dispatcher(AppDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(url: String): Resource<Repository> {
        return withContext(ioDispatcher) {
            safeCall {
                val repository = url.toValidRepositoryLink()
                val providersBranch = repository.getRawLink(
                    filename = "updater.json",
                    branch = "builds"
                )

                if (!isProvidersBranchValid(providersBranch)) {
                    return@withContext Resource.Failure(UtilR.string.invalid_repository)
                }

                return@withContext Resource.Success(repository)
            } ?: Resource.Failure(UtilR.string.invalid_repo_link)
        }
    }

    private fun isProvidersBranchValid(branchUrl: String): Boolean {
        val response = client.request(branchUrl).execute()

        return response.isSuccessful
    }
}