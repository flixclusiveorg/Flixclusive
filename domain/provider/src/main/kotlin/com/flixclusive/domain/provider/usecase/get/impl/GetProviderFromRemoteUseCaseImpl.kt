package com.flixclusive.domain.provider.usecase.get.impl

import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.exception.ExceptionWithUiText
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.util.exception.actualMessage
import com.flixclusive.core.util.network.json.fromJson
import com.flixclusive.core.util.network.okhttp.request
import com.flixclusive.domain.provider.R
import com.flixclusive.domain.provider.usecase.get.GetProviderFromRemoteUseCase
import com.flixclusive.domain.provider.util.Constants
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.model.provider.Repository
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.util.Collections
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private typealias UpdaterJsonUrl = String

internal class GetProviderFromRemoteUseCaseImpl
    @Inject
    constructor(
        private val client: OkHttpClient,
        private val appDispatchers: AppDispatchers,
    ) : GetProviderFromRemoteUseCase {
        /**
         * A cache for the parsed updater JSON file.
         *
         * This cache is used to avoid unnecessary network requests
         * and to improve the performance of the application.
         * */
        private val cachedProviders: MutableMap<UpdaterJsonUrl, CachedUpdaterJsonFile> =
            Collections.synchronizedMap(HashMap())

        override suspend fun invoke(
            repository: Repository,
            id: String,
        ): Resource<ProviderMetadata> {
            return when (val result = invoke(repository)) {
                is Resource.Success -> {
                    val provider = result.data?.firstOrNull { it.id == id }

                    return if (provider != null) {
                        Resource.Success(provider)
                    } else {
                        Resource.Failure(
                            ExceptionWithUiText(
                                uiText = UiText.from(R.string.provider_not_found_message),
                                cause = NullPointerException(),
                            ),
                        )
                    }
                }

                is Resource.Failure -> Resource.Failure(result.error)
                Resource.Loading -> Resource.Loading
            }
        }

        override suspend operator fun invoke(repository: Repository): Resource<List<ProviderMetadata>> {
            return withContext(appDispatchers.io) {
                try {
                    val updaterJsonUrl =
                        repository.getRawLink(
                            filename = Constants.UPDATER_FILE,
                            branch = "builds", // TODO: This should not be hardcoded, consider making it configurable
                        )

                    val providersFromRemote = getProvidersFromUrl(updaterJsonUrl)

                    if (providersFromRemote.isEmpty()) {
                        val e = NullPointerException()

                        throw ExceptionWithUiText(
                            uiText = UiText.from(R.string.empty_repository_message),
                            cause = e,
                            message = e.actualMessage,
                        )
                    }

                    Resource.Success(providersFromRemote)
                } catch (e: Throwable) {
                    return@withContext Resource.Failure(e)
                }
            }
        }

        /**
         * Fetches the list of [ProviderMetadata] from the given updater JSON URL.
         *
         * This method caches the result for 30 minutes to avoid unnecessary network requests.
         * If the cached data is expired, it will fetch the data again from the URL.
         *
         * @param updaterJsonUrl The URL of the updater JSON file.
         *
         * @return The list of [ProviderMetadata] parsed from the JSON file.
         *
         * @throws ExceptionWithUiText if the request fails or the JSON parsing fails.
         * */
        private fun getProvidersFromUrl(updaterJsonUrl: String): List<ProviderMetadata> {
            val cached = cachedProviders[updaterJsonUrl]

            return try {
                if (cached != null && !cached.isExpired) {
                    cached.data
                } else {
                    val response = client.request(updaterJsonUrl).execute()

                    when (response.code) {
                        200 -> {
                            response.fromJson<List<ProviderMetadata>>()
                                .also {
                                    cachedProviders[updaterJsonUrl] = CachedUpdaterJsonFile(it)
                                }
                        }
                        404 -> {
                            throw ExceptionWithUiText(
                                uiText = UiText.from(R.string.repository_not_found_message),
                                cause = NullPointerException(),
                                message = "Response code: ${response.code}",
                            )
                        }
                        else -> {
                            throw ExceptionWithUiText(
                                uiText = UiText.from(R.string.failed_repository_fetch_message),
                                cause = NullPointerException(),
                                message = "Response code: ${response.code}",
                            )
                        }
                    }
                }
            } catch (e: Throwable) {
                if (e is ExceptionWithUiText) {
                    throw e
                }

                throw ExceptionWithUiText(
                    uiText = UiText.from(R.string.failed_repository_fetch_message),
                    cause = e,
                    message = e.actualMessage,
                )
            }
        }

        /**
         * A data class to cache the parsed JSON file from the updater URL.
         *
         * It contains the list of [ProviderMetadata] and the time when it was fetched.
         * The data is considered expired if it was fetched more than 30 minutes ago.
         * */
        private class CachedUpdaterJsonFile(
            val data: List<ProviderMetadata>,
            val time: Long = System.currentTimeMillis(),
        ) {
            val isExpired get() = time < System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(30)
        }
    }
