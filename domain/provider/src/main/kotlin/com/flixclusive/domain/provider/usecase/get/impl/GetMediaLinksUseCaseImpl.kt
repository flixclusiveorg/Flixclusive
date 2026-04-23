package com.flixclusive.domain.provider.usecase.get.impl

import android.content.Context
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.common.provider.LoadLinksState
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.core.network.util.Resource.Failure.Companion.toNetworkException
import com.flixclusive.core.util.coroutines.mapAsync
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.core.util.log.warnLog
import com.flixclusive.data.provider.repository.MediaLinks
import com.flixclusive.data.provider.repository.MediaLinksCacheKey
import com.flixclusive.data.provider.repository.MediaLinksRepository
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.domain.provider.R
import com.flixclusive.domain.provider.usecase.get.GetMediaLinksUseCase
import com.flixclusive.domain.provider.util.extensions.sendCrossMatchingMessage
import com.flixclusive.domain.provider.util.extensions.sendExtractingLinksMessage
import com.flixclusive.domain.provider.util.extensions.sendFetchingFilmMessage
import com.flixclusive.model.film.FilmMetadata
import com.flixclusive.model.film.common.tv.Episode
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.model.provider.link.Stream
import com.flixclusive.model.provider.link.Subtitle
import com.flixclusive.provider.capability.CrossMatchProviderApi
import com.flixclusive.provider.capability.CrossMatchProviderApi.Companion.canHandle
import com.flixclusive.provider.capability.MediaLinkProviderApi
import com.flixclusive.provider.capability.MediaLinkType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class GetMediaLinksUseCaseImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val mediaLinksRepository: MediaLinksRepository,
    private val userSessionDataStore: UserSessionDataStore,
    private val providerRepository: ProviderRepository,
) : GetMediaLinksUseCase {
    override operator fun invoke(
        film: FilmMetadata,
        episode: Episode?,
    ) = channelFlow {
        val userId = userSessionDataStore.currentUserId.filterNotNull().first()
        val enabledProviders = providerRepository.getEnabledProviders(ownerId = userId)
        val oldCache = mediaLinksRepository.getLinks(
            MediaLinksCacheKey.create(
                filmId = film.id,
                providerId = film.providerId,
                episode = episode,
            )
        )

        if (oldCache?.isReady == true) {
            send(LoadLinksState.Success(providerId = oldCache.providerId))
            return@channelFlow
        }

        if (enabledProviders.isEmpty()) {
            send(
                LoadLinksState.Unavailable(
                    UiText.from(R.string.get_media_links_error_empty_provider_list)
                )
            )
            return@channelFlow
        }

        val provider = providerRepository.getProvider(film.providerId, userId)
        if (provider == null) {
            warnLog("Failed to fetch media links: no provider plugin found for id: ${film.providerId}")
            send(
                LoadLinksState.Unavailable(
                    UiText.from(
                        R.string.get_media_links_error_no_provider_plugin,
                        film.providerId
                    )
                )
            )
            return@channelFlow
        }

        val mediaLinksApi = provider.plugin?.getMediaLinkApi(context)
        if (mediaLinksApi != null) {
            processProvider(
                film = film,
                id = provider.id,
                mediaLinksApi = mediaLinksApi,
                metadata = provider.metadata!!,
            )
            return@channelFlow
        }

        if (film.externalIds.isEmpty()) {
            warnLog("Failed to fetch media links: no external IDs found for film with id: ${film.id}")
            send(LoadLinksState.Unavailable())
            return@channelFlow
        }

        val combinedApis = enabledProviders.filter { enabledProvider ->
            val crossMatchApi = enabledProvider.plugin?.getCrossMatchApi(context)
            val mediaLinkApi = enabledProvider.plugin?.getMediaLinkApi(context)

            crossMatchApi?.canHandle(film) == true && mediaLinkApi != null
        }.map {
            val crossMatchApi = it.plugin!!.getCrossMatchApi(context)!!
            val mediaLinkApi = it.plugin!!.getMediaLinkApi(context)!!

            Triple(it.metadata!!, crossMatchApi, mediaLinkApi)
        }

        if (combinedApis.isEmpty()) {
            warnLog("Failed to fetch media links: no cross-matcher API found among enabled providers")
            send(LoadLinksState.Unavailable())
            return@channelFlow
        }

        val subtitlesOnlyApi = combinedApis.filter { (_, _, mediaLinkApi) ->
            mediaLinkApi.supportedLinkTypes.size == 1
                && mediaLinkApi.supportedLinkTypes.contains(MediaLinkType.SUBTITLES)
        }

        launch {
            subtitlesOnlyApi.mapAsync { (provider, crossMatcherApi, mediaLinkApi) ->
                sendCrossMatchingMessage(provider)
                val crossMatchedFilm = getCrossMatchedFilm(film, crossMatcherApi)
                if (crossMatchedFilm == null) {
                    warnLog("Cross-matching failed for subtitle-only provider ${provider.name} with film ${film.title} (${film.id})")
                    return@mapAsync null
                }

                val success = processProvider(
                    film = crossMatchedFilm,
                    id = provider.id,
                    mediaLinksApi = mediaLinkApi,
                    metadata = provider,
                )

                if (!success) {
                    warnLog("Failed to fetch subtitles from provider ${provider.name} for film ${film.title} (${film.id})")
                }
            }
        }

        val streamProviders = combinedApis - subtitlesOnlyApi.toSet()
        streamProviders.forEach { (provider, crossMatcherApi, mediaLinkApi) ->
            val crossMatchedFilm = getCrossMatchedFilm(film, crossMatcherApi)
            if (crossMatchedFilm == null) {
                warnLog("Cross-matching failed for stream links provider ${provider.name} with film ${film.title} (${film.id})")
                return@forEach
            }

            val success = processProvider(
                film = crossMatchedFilm,
                id = provider.id,
                mediaLinksApi = mediaLinkApi,
                metadata = provider,
            )

            if (success) {
                // If we successfully found stream links, we can stop here and not check subtitle-only providers
                return@channelFlow
            }
        }
    }

    private suspend fun ProducerScope<LoadLinksState>.processProvider(
        id: String,
        film: FilmMetadata,
        episode: Episode? = null,
        metadata: ProviderMetadata,
        mediaLinksApi: MediaLinkProviderApi,
        quiet: Boolean = false,
    ): Boolean {
        if (quiet) {
            sendFetchingFilmMessage(provider = metadata.name)
        }

        val key = MediaLinksCacheKey.create(
            providerId = id,
            filmId = film.id,
            episode = episode,
        )

        // Check if the cache already exists for this provider
        val mediaLinks = mediaLinksRepository.getLinks(key)
            ?: MediaLinks(
                watchId = film.id,
                providerId = id,
                thumbnail = film.backdropImage ?: film.posterImage,
            )

        if (mediaLinks.isReady) {
            send(LoadLinksState.Success(providerId = mediaLinks.providerId))
            return true
        }

        mediaLinksRepository.insertLinks(mediaLinks = mediaLinks, key = key)

        sendExtractingLinksMessage(provider = metadata)

        try {
            mediaLinksApi.getLinks(
                film = film,
                episode = episode,
            ).onCompletion { error ->
                if (error != null) {
                    throw error
                }

                val mediaLinks = mediaLinksRepository.getLinks(key)
                if (mediaLinks != null && mediaLinks.hasStreamableLinks) {
                    mediaLinksRepository.insertLinks(key, mediaLinks.copy(hasExtractedSuccessfully = true))
                    send(LoadLinksState.Success(providerId = mediaLinks.providerId))
                } else {
                    send(
                        LoadLinksState.Error(
                            UiText.from(R.string.no_links_loaded_format_message, metadata.name),
                        ),
                    )
                }
            }.collect { link ->
                when (link) {
                    is Stream -> mediaLinksRepository.addStream(key, link)
                    is Subtitle -> mediaLinksRepository.addSubtitle(key, link)
                }
            }

            return true
        } catch (e: Throwable) {
            errorLog("Failed to get media links from provider ${metadata.name} for film ${film.title} (${film.id})")
            errorLog(e)

            val parsedError = e.toNetworkException()
            send(LoadLinksState.Error(parsedError.error))
            return false
        }
    }

    private suspend fun getCrossMatchedFilm(
        film: FilmMetadata,
        crossMatcherApi: CrossMatchProviderApi,
    ): FilmMetadata? {
        var crossMatchedFilm = crossMatcherApi.getById(film.externalIds)
        if (crossMatchedFilm == null) {
            crossMatchedFilm = crossMatcherApi.getByFuzzy(film)
        }

        return crossMatchedFilm
    }
}
