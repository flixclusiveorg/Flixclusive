package com.flixclusive.domain.provider

import com.flixclusive.core.util.R
import com.flixclusive.core.util.common.dispatcher.AppDispatchers
import com.flixclusive.core.util.common.dispatcher.Dispatcher
import com.flixclusive.core.util.common.resource.Resource
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.core.util.network.fromJson
import com.flixclusive.core.util.network.request
import com.flixclusive.gradle.entities.ProviderData
import com.flixclusive.gradle.entities.Repository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import javax.inject.Inject


/**
 * Use case to retrieve a list of online providers from 'updater.json' of 'builds' branch
 *
 * This use case retrieves data from a remote repository using an OkHttpClient and suspends the
 * execution until the data is fetched. The data is then returned as a [Resource] object.
 *
 * @param client The OkHttpClient instance used to make network requests.
 * @param ioDispatcher The dispatcher used to run the network call on a background thread.
 */
class GetOnlineProvidersUseCase @Inject constructor(
    private val client: OkHttpClient,
    @Dispatcher(AppDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(repository: Repository): Resource<List<ProviderData>> {
        return withContext(ioDispatcher) {
            safeCall {
                val updaterJsonUrl = repository.getRawLink(
                    filename = "updater.json",
                    branch = "builds"
                )

                val onlineProviders = parseOnlineProviders(updaterJsonUrl)

                if (onlineProviders.isNullOrEmpty()) {
                    throw NullPointerException("This repository does not seem to have providers listed on its 'updater.json'. Make sure this repository follows the proper provider file structure.")
                }


                return@withContext Resource.Success(onlineProviders)
            } ?: Resource.Failure(R.string.failed_to_load_online_providers)
        }
    }

    private fun parseOnlineProviders(
        updaterJsonUrl: String
    ): List<ProviderData>? {
        val response = client.request(updaterJsonUrl).execute()

        return response.body?.string()?.let(::fromJson)
    }
}