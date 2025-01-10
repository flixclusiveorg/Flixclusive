package com.flixclusive.domain.provider

import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.util.coroutines.AppDispatchers.Companion.withIOContext
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.core.util.network.json.fromJson
import com.flixclusive.core.util.network.okhttp.request
import com.flixclusive.model.provider.ProviderMetadata
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
class GetOnlineProvidersUseCase
    @Inject
    constructor(
        private val client: OkHttpClient,
    ) {
    /*
     * TODO: Improve this. It can literally
     *  just use String.format(ProviderMetadata.buildUrl, UPDATER_FILE)
     * */
        suspend operator fun invoke(repository: Repository): Resource<List<ProviderMetadata>> {
            return withIOContext {
                safeCall {
                    val updaterJsonUrl =
                        repository.getRawLink(
                            filename = UPDATER_FILE,
                            branch = "builds",
                        )

                    val onlineProviders = parseOnlineProviders(updaterJsonUrl)

                    if (onlineProviders.isNullOrEmpty()) {
                        throw NullPointerException(
                            "This repository does not seem to have providers listed on its 'updater.json'. Make sure this repository follows the proper provider file structure.",
                        )
                    }

                    return@withIOContext Resource.Success(onlineProviders)
                } ?: Resource.Failure(LocaleR.string.failed_to_load_online_providers)
            }
        }

        private fun parseOnlineProviders(updaterJsonUrl: String): List<ProviderMetadata>? {
            val response = client.request(updaterJsonUrl).execute()

            return response.fromJson(
                "Couldn't parse providers from remote source!",
            )
        }
    }
