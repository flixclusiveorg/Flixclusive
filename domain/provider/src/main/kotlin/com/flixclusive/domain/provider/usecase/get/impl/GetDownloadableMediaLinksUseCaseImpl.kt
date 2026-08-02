package com.flixclusive.domain.provider.usecase.get.impl

import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.DataStoreManager.Companion.getUserPrefs
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.core.datastore.model.user.SubtitlesPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.data.provider.repository.MediaLinksRepository
import com.flixclusive.data.provider.util.extensions.toStream
import com.flixclusive.data.provider.util.extensions.toSubtitle
import com.flixclusive.domain.provider.R
import com.flixclusive.domain.provider.usecase.get.DownloadableMediaLinks
import com.flixclusive.domain.provider.usecase.get.GetDownloadableMediaLinksUseCase
import com.flixclusive.domain.provider.usecase.get.GetMediaLinksUseCase
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.common.tv.Episode
import com.flixclusive.model.provider.link.Subtitle
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.last
import javax.inject.Inject

internal class GetDownloadableMediaLinksUseCaseImpl @Inject constructor(
    private val getMediaLinksUseCase: GetMediaLinksUseCase,
    private val mediaLinksRepository: MediaLinksRepository,
    private val userSessionDataStore: UserSessionDataStore,
    private val dataStoreManager: DataStoreManager,
) : GetDownloadableMediaLinksUseCase {
    override suspend fun invoke(
        media: MediaMetadata,
        episode: Episode?,
    ): Async<DownloadableMediaLinks> {
        val finalState = getMediaLinksUseCase(media, episode).last()
        if (!finalState.isSuccess) {
            return Async.Failure(finalState.message)
        }

        val userId = userSessionDataStore.currentUserId.filterNotNull().first()
        val cachedLinks = mediaLinksRepository.getLinks(
            ownerId = userId,
            mediaId = media.id,
            episodeNumber = episode?.number,
            seasonNumber = episode?.season,
        )

        val streams = cachedLinks
            .flatMap { it.streams }
            .filter { it.isValid }
            .map { it.toStream() }

        if (streams.isEmpty()) {
            return Async.Failure(UiText.from(R.string.get_downloadable_links_error_no_streams))
        }

        val subtitles = cachedLinks.flatMap { it.subtitles }.map { it.toSubtitle() }
        val subtitle = pickPreferredSubtitle(subtitles)

        return Async.Success(DownloadableMediaLinks(streams = streams, subtitle = subtitle))
    }

    private suspend fun pickPreferredSubtitle(subtitles: List<Subtitle>): Subtitle? {
        if (subtitles.isEmpty()) return null

        val preferredLanguage = dataStoreManager
            .getUserPrefs<SubtitlesPreferences>(UserPreferences.SUBTITLES_PREFS_KEY)
            .subtitleLanguage

        return subtitles.firstOrNull { it.language.equals(preferredLanguage, ignoreCase = true) }
            ?: subtitles.first()
    }
}
