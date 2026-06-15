package com.flixclusive.domain.provider.usecase.tracker.impl

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.flixclusive.core.database.entity.watched.EpisodeProgress
import com.flixclusive.core.database.entity.watched.MovieProgress
import com.flixclusive.core.database.entity.watched.WatchProgress
import com.flixclusive.core.database.entity.watched.WatchStatus
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.core.util.log.warnLog
import com.flixclusive.data.database.repository.WatchProgressRepository
import com.flixclusive.data.provider.ProviderCapability
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.domain.provider.usecase.get.GetCrossMatchedMediaMetadataUseCase
import com.flixclusive.domain.provider.usecase.tracker.SyncFromScrobblersUseCase
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.Show
import com.flixclusive.model.media.common.MediaType
import com.flixclusive.model.media.common.tv.Episode
import com.flixclusive.model.media.common.tv.Season
import com.flixclusive.provider.capability.TrackerFeature
import com.flixclusive.provider.capability.TrackerProviderApi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import java.util.Date
import javax.inject.Inject

private val Context.syncCacheTtl: DataStore<Preferences> by preferencesDataStore("scrobbler_sync_cache_ttl")

internal class SyncFromScrobblersUseCaseImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val userSessionDataStore: UserSessionDataStore,
    private val providerRepository: ProviderRepository,
    private val watchProgressRepository: WatchProgressRepository,
    private val getCrossMatchedMediaMetadata: GetCrossMatchedMediaMetadataUseCase
) : SyncFromScrobblersUseCase {
    private companion object {
        const val CACHE_TTL_KEY_PREFIX = "scrobble_sync_cache_ttl:"
        const val CACHE_TTL_DURATION_MS = 10 * 60 * 1000L

        fun getCacheKey(item: MediaMetadata, episode: Episode?): String {
            return "$CACHE_TTL_KEY_PREFIX${item.id}:${episode?.season}:${episode?.number}"
        }

        suspend fun DataStore<Preferences>.isExpired(key: String): Boolean {
            val lastSyncTime = data.filterNotNull().first()[longPreferencesKey(key)] ?: 0L
            val now = System.currentTimeMillis()
            return now - lastSyncTime >= CACHE_TTL_DURATION_MS
        }

        suspend fun DataStore<Preferences>.updateSyncTime(key: String) {
            edit { prefs ->
                prefs[longPreferencesKey(key)] = System.currentTimeMillis()
            }
        }
    }

    private val syncCacheTtl by lazy { context.syncCacheTtl }

    private data class ScrobblerProgress(
        private val progressPercent: Float,
        val runtime: Int
    ) {
        val absoluteProgress: Long?
            get() {
                return if (progressPercent >= WatchProgress.WATCH_COMPLETED_THRESHOLD && runtime <= 0) {
                    Long.MAX_VALUE
                } else if (runtime <= 0) {
                    null
                } else {
                    (progressPercent / 100 * absoluteRuntime).toLong()
                }
            }

        val absoluteRuntime: Long
            get() {
                return when {
                    runtime < 100 -> runtime.toLong() * 60 * 1000

                    // minutes
                    runtime < 10000 -> runtime.toLong() * 1000

                    // seconds
                    else -> runtime.toLong() // milliseconds
                }
            }
    }

    override suspend fun invoke(
        item: MediaMetadata,
        episode: Episode?
    ) {
        val cacheKey = getCacheKey(item, episode)
        if (!syncCacheTtl.isExpired(cacheKey)) return

        val scrobblers = getScrobblers()

        val progresses = scrobblers.mapNotNull { (providerId, api) ->
            safeCall {
                val (matchedMedia, matchedEpisode) = getCrossMatches(
                    item = item,
                    episode = episode,
                    providerId = providerId
                )

                val progress = api.getScrobbledProgress(
                    item = matchedMedia,
                    episode = matchedEpisode
                )

                ScrobblerProgress(
                    progressPercent = progress,
                    runtime = matchedMedia.runtime ?: item.runtime ?: -1
                )
            }
        }

        val maxProgress = progresses.maxByOrNull {
            it.absoluteProgress ?: 0L
        }

        if (maxProgress == null || maxProgress.absoluteProgress == 0L) {
            syncCacheTtl.updateSyncTime(cacheKey)
            return
        }

        if (episode != null) {
            updateEpisodeProgress(item, episode, maxProgress)
        } else {
            updateMovieProgress(item, maxProgress)
        }

        syncCacheTtl.updateSyncTime(cacheKey)
    }

    private suspend fun updateEpisodeProgress(
        item: MediaMetadata,
        episode: Episode,
        progress: ScrobblerProgress
    ) {
        val userId = userSessionDataStore.currentUserId.filterNotNull().first()
        val absoluteProgress = progress.absoluteProgress ?: return

        var existingProgress = watchProgressRepository.getEpisodeProgress(
            tvShowId = item.id,
            seasonNumber = episode.season,
            episodeNumber = episode.number,
            ownerId = userId
        )

        if (existingProgress != null && existingProgress.progress >= absoluteProgress) {
            return
        }

        if (existingProgress == null) {
            val status = when {
                absoluteProgress >= WatchProgress.WATCH_COMPLETED_THRESHOLD -> WatchStatus.COMPLETED
                else -> WatchStatus.WATCHING
            }

            existingProgress = EpisodeProgress(
                mediaId = item.id,
                ownerId = userId,
                seasonNumber = episode.season,
                episodeNumber = episode.number,
                progress = absoluteProgress,
                status = status
            )
        } else if (existingProgress.progress < absoluteProgress) {
            val newStatus = when {
                absoluteProgress >= WatchProgress.WATCH_COMPLETED_THRESHOLD -> WatchStatus.COMPLETED
                else -> existingProgress.status
            }

            existingProgress = existingProgress.copy(
                status = newStatus,
                progress = absoluteProgress,
                updatedAt = Date()
            )
        }

        watchProgressRepository.insert(
            item = existingProgress,
            media = item
        )
    }

    private suspend fun updateMovieProgress(
        item: MediaMetadata,
        progress: ScrobblerProgress
    ) {
        val userId = userSessionDataStore.currentUserId.filterNotNull().first()
        val absoluteProgress = progress.absoluteProgress ?: return

        var existingProgress = watchProgressRepository
            .get(
                id = item.id,
                ownerId = userId,
                type = MediaType.MOVIE
            )?.watchData as? MovieProgress

        if (existingProgress != null && existingProgress.progress >= absoluteProgress) {
            return
        }

        if (existingProgress == null) {
            val status = when {
                absoluteProgress >= WatchProgress.WATCH_COMPLETED_THRESHOLD -> WatchStatus.COMPLETED
                else -> WatchStatus.WATCHING
            }

            existingProgress = MovieProgress(
                mediaId = item.id,
                ownerId = userId,
                progress = absoluteProgress,
                status = status
            )
        } else if (existingProgress.progress < absoluteProgress) {
            val newStatus = when {
                absoluteProgress >= WatchProgress.WATCH_COMPLETED_THRESHOLD -> WatchStatus.COMPLETED
                else -> existingProgress.status
            }

            existingProgress = existingProgress.copy(
                status = newStatus,
                progress = absoluteProgress,
                updatedAt = Date()
            )
        }

        watchProgressRepository.insert(
            item = existingProgress,
            media = item
        )
    }

    private suspend fun getCrossMatches(
        item: MediaMetadata,
        episode: Episode?,
        providerId: String
    ): Pair<MediaMetadata, Episode?> {
        return if (item.providerId != providerId) {
            val crossMatchedMedia = getCrossMatchedMediaMetadata(item, providerId)

            var crossMatchedEpisode = episode
            if (crossMatchedMedia is Show && episode != null) {
                var season = crossMatchedMedia.getSeason(episode.season)
                if (season is Season.Partial) {
                    val userId = userSessionDataStore.currentUserId.filterNotNull().first()
                    val provider = providerRepository.getProvider(providerId, userId)
                    val metadataApi = provider?.plugin?.getMetadataApi(context)
                        ?: throw IllegalStateException("Metadata API not found for providerId: $providerId")

                    season = metadataApi.getSeason(crossMatchedMedia, season)
                }

                if (season !is Season.Full) {
                    throw IllegalStateException(
                        "Season data is not complete for season ${episode.season} of show ${crossMatchedMedia.title}"
                    )
                }

                crossMatchedEpisode = season.getEpisode(episode.number)
            }

            crossMatchedMedia to crossMatchedEpisode
        } else {
            item to episode
        }
    }

    private suspend fun getScrobblers(): List<Pair<String, TrackerProviderApi>> {
        val userId = userSessionDataStore.currentUserId.filterNotNull().first()
        val providers = providerRepository.getProvidersWithCapability(userId, ProviderCapability.TRACKER)

        return providers.mapNotNull {
            if (!it.isTrackerEnabled) return@mapNotNull null

            val api = try {
                it.plugin?.getTrackerApi(context)
            } catch (e: Throwable) {
                errorLog("Failed to get TrackerProviderApi for provider ${it.name}: ${e.message}")
                e.printStackTrace()
                return@mapNotNull null
            }

            if (api == null) {
                warnLog("Provider ${it.name} does not support tracker operations")
                return@mapNotNull null
            }

            if (!api.isAuthenticated()) {
                warnLog("Provider ${it.name} is not authenticated for tracker operations")
                return@mapNotNull null
            }

            if (!api.getFeatures().contains(TrackerFeature.SCROBBLE)) {
                warnLog("Provider ${it.name} does not support scrobble operations")
                return@mapNotNull null
            }

            it.id to api
        }
    }
}
