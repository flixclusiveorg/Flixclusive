package com.flixclusive.domain.provider.usecase.get.impl

import android.content.Context
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.common.provider.LoadLinksState
import com.flixclusive.core.datastore.UserSessionDataStore
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
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.Show
import com.flixclusive.model.media.common.tv.Episode
import com.flixclusive.model.provider.link.Stream
import com.flixclusive.model.provider.link.Subtitle
import com.flixclusive.provider.capability.CrossMatchProviderApi
import com.flixclusive.provider.capability.MediaLinkProviderApi
import com.flixclusive.provider.capability.MediaLinkType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class GetMediaLinksUseCaseImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val mediaLinksRepository: MediaLinksRepository,
    private val userSessionDataStore: UserSessionDataStore,
    private val providerRepository: ProviderRepository,
    private val appDispatchers: AppDispatchers,
) : GetMediaLinksUseCase {
    override operator fun invoke(
        media: MediaMetadata,
        episode: Episode?,
    ) = channelFlow {
        val userId = userSessionDataStore.currentUserId.filterNotNull().first()
        val providers = providerRepository.getProviders(ownerId = userId)
        val oldCache = mediaLinksRepository.getLinks(
            MediaLinksCacheKey.create(
                mediaId = media.id,
                providerId = media.providerId,
                episode = episode,
            )
        )

        if (oldCache?.isReady == true) {
            send(LoadLinksState.Success(providerId = oldCache.providerId))
            return@channelFlow
        }

        if (providers.isEmpty()) {
            send(
                LoadLinksState.Unavailable(
                    UiText.from(R.string.get_media_links_error_empty_provider_list)
                )
            )
            return@channelFlow
        }

        val provider = providerRepository.getProvider(media.providerId, userId)
        if (provider == null) {
            warnLog("Failed to fetch media links: no provider plugin found for id: ${media.providerId}")
            send(
                LoadLinksState.Unavailable(
                    UiText.from(
                        R.string.get_media_links_error_no_provider_plugin,
                        media.providerId
                    )
                )
            )
            return@channelFlow
        }

        val mediaLinksApi = provider.plugin?.getMediaLinkApi(context)
        if (mediaLinksApi != null && provider.isMediaLinkEnabled) {
            val key = MediaLinksCacheKey.create(
                mediaId = media.id,
                providerId = media.providerId,
                episode = episode,
            )

            sendExtractingLinksMessage(provider.metadata!!)

            val success = try {
                processProvider(
                    key = key,
                    media = media,
                    id = provider.id,
                    mediaLinksApi = mediaLinksApi,
                    episode = episode
                )
            } catch (e: Throwable) {
                errorLog(e)
                send(LoadLinksState.Error(e))
                false
            }

            if (success) {
                send(LoadLinksState.Success(providerId = media.providerId))
                return@channelFlow
            }
        }

        if (media.externalIds.isEmpty()) {
            send(
                LoadLinksState.Unavailable(
                    UiText.from(R.string.no_links_loaded_format_message, provider.name!!),
                ),
            )
            return@channelFlow
        }

        send(LoadLinksState.Fetching(R.string.label_check_cross_match_providers))
        val combinedApis = providers.filter { enabledProvider ->
            val crossMatchApi = enabledProvider.plugin?.getCrossMatchApi(context)
            val mediaLinkApi = enabledProvider.plugin?.getMediaLinkApi(context)

            crossMatchApi != null && mediaLinkApi != null
                && enabledProvider.isMediaLinkEnabled && enabledProvider.isCrossMatchEnabled
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
                val crossMatchedMedia = getCrossMatchedMedia(media, crossMatcherApi)
                if (crossMatchedMedia == null) {
                    warnLog("Cross-matching failed for subtitle-only provider ${provider.name} with media ${media.title} (${media.id})")
                    return@mapAsync null
                }

                val success = try {
                    processProvider(
                        key = MediaLinksCacheKey.create(
                            mediaId = media.id,
                            providerId = provider.id,
                            episode = episode,
                        ),
                        media = crossMatchedMedia,
                        id = provider.id,
                        mediaLinksApi = mediaLinkApi,
                        episode = episode,
                    )
                } catch (e: Throwable) {
                    errorLog(e)
                    false
                }

                if (!success) {
                    warnLog("Failed to fetch subtitles from provider ${provider.name} for media ${media.title} (${media.id})")
                }
            }
        }

        launch {
            val streamProviders = combinedApis - subtitlesOnlyApi.toSet()
            streamProviders.forEach { (provider, crossMatcherApi, mediaLinkApi) ->
                val crossMatchedMedia = getCrossMatchedMedia(media, crossMatcherApi)
                if (crossMatchedMedia == null) {
                    warnLog("Cross-matching failed for stream links provider ${provider.name} with media ${media.title} (${media.id})")
                    return@forEach
                }

                sendCrossMatchingMessage(provider)

                val crossMatchedEpisode = if (crossMatchedMedia is Show && episode != null) {
                    val season = crossMatchedMedia.getSeason(episode.season)
                    season?.getEpisode(episode.number)
                } else {
                    null
                }

                if (crossMatchedMedia is Show && crossMatchedEpisode == null) {
                    send(LoadLinksState.Unavailable())
                    return@forEach
                }

                val success = try {
                    processProvider(
                        key = MediaLinksCacheKey.create(
                            mediaId = media.id,
                            providerId = provider.id,
                            episode = episode,
                        ),
                        media = crossMatchedMedia,
                        episode = crossMatchedEpisode,
                        id = provider.id,
                        mediaLinksApi = mediaLinkApi,
                    )
                } catch (e: Throwable) {
                    errorLog(e)
                    false
                }

                if (success) {
                    return@launch
                }
            }

            send(LoadLinksState.Unavailable())
        }
    }.flowOn(appDispatchers.io)

    private suspend fun ProducerScope<LoadLinksState>.processProvider(
        id: String,
        media: MediaMetadata,
        key: MediaLinksCacheKey,
        episode: Episode? = null,
        mediaLinksApi: MediaLinkProviderApi,
    ): Boolean {
        // Check if the cache already exists for this provider
        val mediaLinks = mediaLinksRepository.getLinks(key)
            ?: MediaLinks(
                watchId = media.id,
                providerId = id,
                thumbnail = media.backdropImage ?: media.posterImage,
            )

        if (mediaLinks.isReady) {
            return true
        }

        mediaLinksRepository.insertLinks(mediaLinks = mediaLinks, key = key)

        mediaLinksApi.getLinks(
            media = media,
            episode = episode,
            onLinkFound = { link ->
                when (link) {
                    is Stream -> mediaLinksRepository.addStream(key, link)
                    is Subtitle -> mediaLinksRepository.addSubtitle(key, link)
                }
            },
        )

        val updatedLinks = mediaLinksRepository.getLinks(key)
        if (updatedLinks == null || !updatedLinks.hasValidLinks) {
            return false
        }

        mediaLinksRepository.insertLinks(
            key = key,
            mediaLinks = updatedLinks.copy(hasExtractedSuccessfully = true)
        )
        return true
    }

    private suspend fun getCrossMatchedMedia(
        media: MediaMetadata,
        crossMatcherApi: CrossMatchProviderApi,
    ): MediaMetadata? {
        try {
            var crossMatchedMedia = crossMatcherApi.getById(media.externalIds)
            if (crossMatchedMedia == null) {
                crossMatchedMedia = crossMatcherApi.getByFuzzy(media)
            }

            return crossMatchedMedia
        } catch (e: Throwable) {
            errorLog("Cross-matching failed for media ${media.title} (${media.id})}")
            errorLog(e)
            return null
        }
    }
}
