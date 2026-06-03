package com.flixclusive.domain.provider.usecase.get.impl

import android.content.Context
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.common.provider.LoadLinksState
import com.flixclusive.core.database.entity.media.DBMedia.Companion.toDBMedia
import com.flixclusive.core.database.entity.provider.CachedMediaLinks
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.core.util.coroutines.mapAsync
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.core.util.log.warnLog
import com.flixclusive.data.provider.repository.MediaLinksRepository
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.data.provider.util.extensions.toDBStream
import com.flixclusive.data.provider.util.extensions.toDBSubtitle
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
import kotlinx.coroutines.coroutineScope
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

        val existingCache = mediaLinksRepository.getLinks(
            ownerId = userId,
            mediaId = media.id,
            episodeNumber = episode?.number,
            seasonNumber = episode?.season
        )

        if (existingCache != null && existingCache.hasValidLinks) {
            send(LoadLinksState.Success)
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

        val cache = existingCache?.cache ?: CachedMediaLinks(
            providerId = provider.id,
            ownerId = userId,
            mediaId = media.id,
            episodeNumber = episode?.number,
            seasonNumber = episode?.season,
            thumbnail = media.backdropImage ?: media.posterImage,
        )

        if (existingCache == null) {
            mediaLinksRepository.insertCache(
                entry = cache,
                media = media.toDBMedia()
            )
        }

        val mediaLinksApi = provider.plugin?.getMediaLinkApi(context)
        if (mediaLinksApi != null && provider.isMediaLinkEnabled) {
            sendExtractingLinksMessage(provider.metadata!!)

            val success = try {
                processProvider(
                    cacheId = cache.id,
                    media = media,
                    episode = episode,
                    mediaLinksApi = mediaLinksApi,
                )
            } catch (e: Throwable) {
                errorLog(e)
                send(LoadLinksState.Error(e))
                false
            }

            if (success) {
                send(LoadLinksState.Success)
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
        val combinedApis = providers.mapNotNull {
            if (it.id == provider.id) return@mapNotNull null
            if (!it.isCrossMatchEnabled || !it.isMediaLinkEnabled) return@mapNotNull null

            val plugin = it.plugin
            val metadata = it.metadata
            if (metadata == null || plugin == null) return@mapNotNull null

            val crossMatchApi = plugin.getCrossMatchApi(context) ?: return@mapNotNull null
            val mediaLinkApi = plugin.getMediaLinkApi(context) ?: return@mapNotNull null

            Triple(metadata, crossMatchApi, mediaLinkApi)
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
            subtitlesOnlyApi.mapAsync { (providerMeta, crossMatcherApi, mediaLinkApi) ->
                val crossMatchedMedia = getCrossMatchedMedia(media, crossMatcherApi)
                if (crossMatchedMedia == null) {
                    warnLog("Cross-matching failed for subtitle-only provider ${providerMeta.name} with media ${media.title} (${media.id})")
                    return@mapAsync null
                }

                val crossMatchedEpisode = if (crossMatchedMedia is Show && episode != null) {
                    val season = crossMatchedMedia.getSeason(episode.season)
                    season?.getEpisode(episode.number)
                } else {
                    null
                }

                val success = try {
                    processProvider(
                        cacheId = cache.id,
                        media = crossMatchedMedia,
                        episode = crossMatchedEpisode,
                        mediaLinksApi = mediaLinkApi,
                    )
                } catch (e: Throwable) {
                    errorLog(e)
                    false
                }

                if (!success) {
                    warnLog("Failed to fetch subtitles from provider ${providerMeta.name} for media ${media.title} (${media.id})")
                }
            }
        }

        launch {
            val streamProviders = combinedApis - subtitlesOnlyApi.toSet()
            streamProviders.forEach { (providerMeta, crossMatcherApi, mediaLinkApi) ->
                val crossMatchedMedia = getCrossMatchedMedia(media, crossMatcherApi)
                if (crossMatchedMedia == null) {
                    warnLog("Cross-matching failed for stream links provider ${providerMeta.name} with media ${media.title} (${media.id})")
                    return@forEach
                }

                sendCrossMatchingMessage(providerMeta)

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
                        cacheId = cache.id,
                        media = crossMatchedMedia,
                        episode = crossMatchedEpisode,
                        mediaLinksApi = mediaLinkApi,
                    )
                } catch (e: Throwable) {
                    errorLog(e)
                    false
                }

                if (success) {
                    send(LoadLinksState.Success)
                    return@launch
                }
            }

            send(LoadLinksState.Unavailable())
        }
    }.flowOn(appDispatchers.io)

    private suspend fun processProvider(
        cacheId: String,
        media: MediaMetadata,
        episode: Episode?,
        mediaLinksApi: MediaLinkProviderApi,
    ): Boolean {
        coroutineScope {
            mediaLinksApi.getLinks(
                media = media,
                episode = episode,
                onLinkFound = { link ->
                    launch {
                        mediaLinksRepository.upsertLink(
                            when (link) {
                                is Stream -> link.toDBStream(cacheId)
                                is Subtitle -> link.toDBSubtitle(cacheId)
                            }
                        )
                    }
                },
            )
        }

        val updatedLinks = mediaLinksRepository.getLinksById(cacheId)
        return updatedLinks != null && updatedLinks.hasValidLinks
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
