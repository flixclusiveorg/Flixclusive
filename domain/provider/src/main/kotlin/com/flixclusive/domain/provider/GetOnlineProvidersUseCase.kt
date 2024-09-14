package com.flixclusive.domain.provider

import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.util.coroutines.AppDispatchers.Companion.withIOContext
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.core.util.network.json.fromJson
import com.flixclusive.core.util.network.okhttp.request
import com.flixclusive.model.provider.ProviderData
import com.flixclusive.model.provider.Repository
import okhttp3.OkHttpClient
import javax.inject.Inject
import com.flixclusive.core.locale.R as LocaleR


/**
 * Use case to retrieve a list of online providers from 'updater.json' of 'builds' branch
 *
 * This use case retrieves data from a remote repository using an OkHttpClient and suspends the
 * execution until the data is fetched. The data is then returned as a [Resource] object.
 *
 * @param client The OkHttpClient instance used to make network requests.
 */
class GetOnlineProvidersUseCase @Inject constructor(
    private val client: OkHttpClient
) {
    suspend operator fun invoke(repository: Repository): Resource<List<ProviderData>> {
        return withIOContext {
            safeCall {
                val updaterJsonUrl = repository.getRawLink(
                    filename = "updater.json",
                    branch = "builds"
                )

                val onlineProviders = parseOnlineProviders(updaterJsonUrl)

                if (onlineProviders.isNullOrEmpty()) {
                    throw NullPointerException("This repository does not seem to have providers listed on its 'updater.json'. Make sure this repository follows the proper provider file structure.")
                }


                return@withIOContext Resource.Success(onlineProviders)
            } ?: Resource.Failure(LocaleR.string.failed_to_load_online_providers)
        }
    }

    private fun parseOnlineProviders(
        updaterJsonUrl: String
    ): List<ProviderData>? {
        val response = client.request(updaterJsonUrl).execute()

        return response.body?.string()?.let(::fromJson)
    }
}