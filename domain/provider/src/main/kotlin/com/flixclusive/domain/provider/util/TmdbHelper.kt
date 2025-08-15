package com.flixclusive.domain.provider.util

import com.flixclusive.core.common.provider.MediaLinkResourceState
import com.flixclusive.core.database.entity.WatchHistory
import com.flixclusive.core.database.entity.util.getNextEpisodeToWatch
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.strings.R
import com.flixclusive.data.tmdb.repository.TMDBRepository
import com.flixclusive.domain.provider.util.cache.CachedLinksHelper
import com.flixclusive.model.film.FilmMetadata
import com.flixclusive.model.film.TvShow
import com.flixclusive.model.film.common.tv.Episode
import com.flixclusive.model.provider.link.Stream

internal class TmdbHelper(
    private val tmdbRepository: TMDBRepository,
    private val cachedLinksHelper: CachedLinksHelper,
) {
    private suspend fun loadOfficialWatchProviders(film: FilmMetadata): Resource<List<Stream>> {
        return tmdbRepository.getWatchProviders(
            mediaType = film.filmType.type,
            id = film.tmdbId!!,
        )
    }

    suspend fun extractProviders(
        film: FilmMetadata,
        episode: Episode?,
    ): Resource<Unit> {
        val response = loadOfficialWatchProviders(film)

        return if (response is Resource.Success) {
            cachedLinksHelper.storeTmdbCache(
                film = film,
                episode = episode,
                streams = response.data ?: emptyList(),
            )
            Resource.Success(Unit)
        } else {
            Resource.Failure(response.error)
        }
    }

    /**
     *
     * Obtains the [Episode] data of the nearest
     * episode to be watched by the user
     *
     * */
    suspend fun getNearestEpisodeToWatch(
        film: TvShow,
        watchHistory: WatchHistory?,
    ): Resource<Episode?> {
        val isNewlyWatchShow =
            watchHistory == null || watchHistory.episodesWatched.isEmpty()

        val seasonNumber: Int
        val episodeNumber: Int

        if (isNewlyWatchShow) {
            if (film.totalSeasons == 0 || film.totalEpisodes == 0) {
                return Resource.Failure(MediaLinkResourceState.Unavailable().message)
            }

            seasonNumber = 1
            episodeNumber = 1
        } else {
            val (nextSeason, nextEpisode) = getNextEpisodeToWatch(watchHistory!!)
            seasonNumber = nextSeason ?: 1
            episodeNumber = nextEpisode ?: 1
        }

        return if (film.isFromTmdb) {
            getEpisodeFromTmdb(
                tmdbId = film.tmdbId,
                seasonNumber = seasonNumber,
                episodeNumber = episodeNumber,
            )
        } else {
            val episode =
                film.seasons
                    .find {
                        it.number == seasonNumber
                    }?.episodes
                    ?.find {
                        it.number == episodeNumber
                    }

            if (episode == null) {
                return Resource.Failure(R.string.unavailable_episode)
            }

            Resource.Success(episode)
        }
    }

    private suspend fun getEpisodeFromTmdb(
        tmdbId: Int?,
        seasonNumber: Int,
        episodeNumber: Int,
    ): Resource<Episode?> {
        return tmdbRepository
            .getEpisode(
                id = tmdbId ?: return Resource.Failure(R.string.invalid_tmdb_id),
                seasonNumber = seasonNumber,
                episodeNumber = episodeNumber,
            ).also {
                if (it is Resource.Failure) {
                    return it
                } else if (it is Resource.Success && it.data == null) {
                    return Resource.Failure(R.string.unavailable_episode)
                }
            }
    }
}
