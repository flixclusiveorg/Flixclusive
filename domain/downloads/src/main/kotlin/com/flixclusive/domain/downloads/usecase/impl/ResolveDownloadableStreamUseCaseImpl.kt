package com.flixclusive.domain.downloads.usecase.impl

import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.model.user.DataPreferences
import com.flixclusive.core.datastore.model.user.PlayerPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.core.network.download.LinkProbe
import com.flixclusive.core.network.download.LinkProbeResult
import com.flixclusive.domain.downloads.usecase.ResolveDownloadableStreamUseCase
import com.flixclusive.domain.downloads.util.DownloadLinkRanker
import com.flixclusive.model.provider.link.Stream
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import com.flixclusive.core.strings.R as LocaleR

internal class ResolveDownloadableStreamUseCaseImpl @Inject constructor(
    private val dataStoreManager: DataStoreManager,
    private val linkProbe: LinkProbe,
) : ResolveDownloadableStreamUseCase {
    override suspend fun invoke(streams: List<Stream>): Async<Stream> {
        if (streams.isEmpty()) {
            return Async.Failure(UiText.from(LocaleR.string.no_download_links_available))
        }

        val dataPreferences = dataStoreManager
            .getUserPrefsAsFlow(UserPreferences.DATA_PREFS_KEY, DataPreferences::class)
            .first()
        val playerPreferences = dataStoreManager
            .getUserPrefsAsFlow(UserPreferences.PLAYER_PREFS_KEY, PlayerPreferences::class)
            .first()

        val probed = probeAll(streams)
        val ranked = DownloadLinkRanker.rank(
            candidates = probed,
            mode = dataPreferences.downloadLinkSelectionMode,
            preferredQuality = playerPreferences.quality,
        )

        val resolved = ranked
            .take(MAX_FALLBACK_ATTEMPTS)
            .firstOrNull { (_, result) -> result.isReachable }

        return resolved?.let { (stream, _) -> Async.Success(stream) }
            ?: Async.Failure(UiText.from(LocaleR.string.download_link_resolution_failed))
    }

    private suspend fun probeAll(streams: List<Stream>): List<Pair<Stream, LinkProbeResult>> =
        coroutineScope {
            streams
                .map { stream -> stream to async { linkProbe.probe(stream.url, stream.customHeaders ?: emptyMap()) } }
                .map { (stream, deferred) -> stream to deferred.await() }
        }

    companion object {
        private const val MAX_FALLBACK_ATTEMPTS = 3
    }
}
