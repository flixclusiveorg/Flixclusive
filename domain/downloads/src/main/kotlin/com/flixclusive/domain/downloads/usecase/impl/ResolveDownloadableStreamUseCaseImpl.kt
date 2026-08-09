package com.flixclusive.domain.downloads.usecase.impl

import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.DataStoreManager.Companion.getUserPrefs
import com.flixclusive.core.datastore.model.user.DataPreferences
import com.flixclusive.core.datastore.model.user.PlayerPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.data.downloads.probe.LinkProbe
import com.flixclusive.data.downloads.probe.LinkProbeResult
import com.flixclusive.data.provider.repository.MediaLinksRepository
import com.flixclusive.data.provider.util.extensions.toStream
import com.flixclusive.domain.downloads.usecase.RankedDownloadCandidate
import com.flixclusive.domain.downloads.usecase.ResolveDownloadableStreamUseCase
import com.flixclusive.domain.downloads.util.DownloadLinkRanker
import com.flixclusive.model.provider.link.Stream
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import com.flixclusive.core.strings.R as LocaleR

internal class ResolveDownloadableStreamUseCaseImpl @Inject constructor(
    private val dataStoreManager: DataStoreManager,
    private val mediaLinksRepository: MediaLinksRepository,
    private val linkProbe: LinkProbe,
) : ResolveDownloadableStreamUseCase {
    override suspend fun invoke(
        ownerId: String,
        mediaId: String,
        seasonNumber: Int?,
        episodeNumber: Int?,
    ): Async<RankedDownloadCandidate> {
        val cachedLinks = mediaLinksRepository.getLinks(
            ownerId = ownerId,
            mediaId = mediaId,
            episodeNumber = episodeNumber,
            seasonNumber = seasonNumber,
        )

        val streams = cachedLinks
            .flatMap { it.streams }
            // Third-party gateway links hand off to another site's own web player, not a
            // direct file/manifest URL — there's nothing downloadable to transfer.
            .filter { it.isValid && !it.isThirdPartyGateway }
            .map { it.toStream() }

        if (streams.isEmpty()) {
            return Async.Failure(UiText.from(LocaleR.string.download_error_no_links_available))
        }

        val dataPreferences = dataStoreManager.getUserPrefs<DataPreferences>(UserPreferences.DATA_PREFS_KEY)
        val playerPreferences = dataStoreManager.getUserPrefs<PlayerPreferences>(UserPreferences.PLAYER_PREFS_KEY)

        val probed = probeAll(streams)
        val ranked = DownloadLinkRanker.rank(
            candidates = probed,
            mode = dataPreferences.downloadLinkSelectionMode,
            direction = dataPreferences.downloadLinkSortDirection,
            preferredQuality = playerPreferences.quality,
        )

        for ((stream, result) in ranked) {
            if (result.isReachable) {
                return Async.Success(RankedDownloadCandidate(stream, result.isHls, result.contentLength))
            }

            mediaLinksRepository.setLinkStatus(stream.url, ownerId, isDead = true)
        }

        return Async.Failure(UiText.from(LocaleR.string.download_error_link_resolution_failed))
    }

    private suspend fun probeAll(streams: List<Stream>): List<Pair<Stream, LinkProbeResult>> =
        coroutineScope {
            streams
                .map { stream -> stream to async { linkProbe.probe(stream.url, stream.customHeaders ?: emptyMap()) } }
                .map { (stream, deferred) -> stream to deferred.await() }
        }
}
