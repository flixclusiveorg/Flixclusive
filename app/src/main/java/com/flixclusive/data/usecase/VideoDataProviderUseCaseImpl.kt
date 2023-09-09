package com.flixclusive.data.usecase

import com.flixclusive.R
import com.flixclusive.domain.common.Resource
import com.flixclusive.domain.model.VideoDataDialogState
import com.flixclusive.domain.model.entities.WatchHistoryItem
import com.flixclusive.domain.model.tmdb.Film
import com.flixclusive.domain.model.tmdb.TMDBEpisode
import com.flixclusive.domain.model.tmdb.TvShow
import com.flixclusive.domain.repository.FilmSourcesRepository
import com.flixclusive.domain.repository.TMDBRepository
import com.flixclusive.domain.usecase.VideoDataProviderUseCase
import com.flixclusive.domain.utils.FilmProviderUtils.initializeSubtitles
import com.flixclusive.domain.utils.WatchHistoryUtils.getNextEpisodeToWatch
import com.flixclusive.presentation.utils.FormatterUtils
import com.flixclusive_provider.models.common.MediaServer
import com.flixclusive_provider.models.common.VideoData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class VideoDataProviderUseCaseImpl @Inject constructor(
    private val filmSourcesRepository: FilmSourcesRepository,
    private val tmdbRepository: TMDBRepository
) : VideoDataProviderUseCase {
    override fun invoke(
        film: Film,
        watchHistoryItem: WatchHistoryItem?,
        server: String?,
        mediaId: String?,
        episode: TMDBEpisode?,
        onSuccess: (VideoData, TMDBEpisode?) -> Unit
    ): Flow<VideoDataDialogState> = flow {
        emit(VideoDataDialogState.Fetching)

        val id = mediaId ?: filmSourcesRepository.getMediaId(film)

        if (id.isNullOrEmpty()) {
            emit(VideoDataDialogState.Unavailable())
            return@flow
        }

        val isNewlyWatchShow =
            watchHistoryItem == null || watchHistoryItem.episodesWatched.isEmpty()

        var episodeToUse: TMDBEpisode? = episode
        if(episodeToUse == null && film is TvShow) {
            val seasonNumber: Int
            val episodeNumber: Int

            if (isNewlyWatchShow) {
                if (film.totalSeasons == 0 || film.totalEpisodes == 0) {
                    return@flow emit(VideoDataDialogState.Unavailable())
                }

                seasonNumber = 1
                episodeNumber = 1
            } else {
                val lastEpisodeWatched = getNextEpisodeToWatch(watchHistoryItem!!)
                seasonNumber = lastEpisodeWatched.first ?: 1
                episodeNumber = lastEpisodeWatched.second ?: 1
            }

            val episodeFromApiService = tmdbRepository.getEpisode(
                id = film.id,
                seasonNumber = seasonNumber,
                episodeNumber = episodeNumber
            )

            if (episodeFromApiService == null) {
                emit(VideoDataDialogState.Unavailable(R.string.unavailable_episode))
                return@flow
            }

            episodeToUse = episodeFromApiService
        }

        emit(VideoDataDialogState.Extracting)
        val titleToUse = FormatterUtils.formatPlayerTitle(film, episodeToUse)
        val serverToUse = server ?: MediaServer.values().first().serverName
        val episodeId = filmSourcesRepository.getEpisodeId(
            mediaId = id,
            filmType = film.filmType,
            episode = episodeToUse?.episode,
            season = episodeToUse?.season
        ) ?: return@flow emit(VideoDataDialogState.Unavailable(R.string.unavailable_episode))

        val videoData = filmSourcesRepository.getStreamingLinks(
            mediaId = id,
            episodeId = episodeId,
            server = serverToUse
        )

        when (videoData) {
            is Resource.Failure -> {
                emit(VideoDataDialogState.Error(videoData.error))
            }
            Resource.Loading -> Unit
            is Resource.Success -> {
                val data = videoData.data
                if (data != null) {
                    emit(VideoDataDialogState.Success)
                    onSuccess(
                        data.copy(title = titleToUse)
                            .initializeSubtitles(),
                        episodeToUse
                    )
                } else {
                    emit(VideoDataDialogState.Unavailable())
                }
            }
        }
    }
}