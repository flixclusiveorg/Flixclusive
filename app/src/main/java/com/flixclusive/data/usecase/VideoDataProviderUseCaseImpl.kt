package com.flixclusive.data.usecase

import com.flixclusive.domain.common.Resource
import com.flixclusive.domain.model.consumet.VideoData
import com.flixclusive.domain.model.entities.WatchHistoryItem
import com.flixclusive.domain.model.tmdb.Film
import com.flixclusive.domain.model.tmdb.FilmType
import com.flixclusive.domain.model.tmdb.TMDBEpisode
import com.flixclusive.domain.model.tmdb.TvShow
import com.flixclusive.domain.repository.ConsumetRepository
import com.flixclusive.domain.repository.TMDBRepository
import com.flixclusive.domain.usecase.VideoDataProviderUseCase
import com.flixclusive.presentation.common.Formatter
import com.flixclusive.presentation.common.Functions
import com.flixclusive.presentation.common.VideoDataDialogState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class VideoDataProviderUseCaseImpl @Inject constructor(
    private val consumetRepository: ConsumetRepository,
    private val tmdbRepository: TMDBRepository,
) : VideoDataProviderUseCase {
    override fun invoke(
        film: Film,
        watchHistoryItem: WatchHistoryItem?,
        consumetId: String?,
        episode: TMDBEpisode?,
        onSuccess: (VideoData, TMDBEpisode?) -> Unit
    ): Flow<VideoDataDialogState> = flow {
        emit(VideoDataDialogState.FETCHING)

        val mediaId: String? = consumetId ?: consumetRepository.getConsumetFilmMediaId(film)

        if (mediaId == null) {
            emit(VideoDataDialogState.ERROR)
            return@flow
        } else if (mediaId.isEmpty()) {
            emit(VideoDataDialogState.UNAVAILABLE)
            return@flow
        }

        val isNewlyWatchShow =
            watchHistoryItem == null || watchHistoryItem.episodesWatched.isEmpty()

        var episodeToUse: TMDBEpisode? = episode
        if(episodeToUse == null && film is TvShow){
            val seasonNumber: Int
            val episodeNumber: Int

            if (isNewlyWatchShow) {
                seasonNumber = if (film.totalSeasons > 0) 1 else {
                    emit(VideoDataDialogState.ERROR)
                    return@flow
                }
                episodeNumber = if (film.totalEpisodes > 0) 1 else {
                    emit(VideoDataDialogState.UNAVAILABLE)
                    return@flow
                }
            } else {
                val lastEpisodeWatched = Functions.getNextEpisodeToWatch(watchHistoryItem!!)
                seasonNumber = lastEpisodeWatched.first ?: 1
                episodeNumber = lastEpisodeWatched.second ?: 1
            }

            if(seasonNumber == 0 || episodeNumber == 0) {
                emit(VideoDataDialogState.ERROR)
                return@flow
            }

            val episodeFromApiService = tmdbRepository.getEpisode(
                id = film.id,
                seasonNumber = seasonNumber,
                episodeNumber = episodeNumber
            )

            if (episodeFromApiService == null) {
                emit(VideoDataDialogState.UNAVAILABLE)
                return@flow
            }

            episodeToUse = episodeFromApiService
        }

        emit(VideoDataDialogState.EXTRACTING)
        val titleToUse = Formatter.formatPlayerTitle(film, episodeToUse)

        val videoData = when (film.filmType) {
            FilmType.MOVIE -> consumetRepository.getMovieStreamingLinks(
                consumetId = mediaId,
                server = consumetRepository.consumetDefaultVideoServer
            )
            FilmType.TV_SHOW -> consumetRepository.getTvShowStreamingLinks(
                consumetId = mediaId,
                episode = episodeToUse!!,
                server = consumetRepository.consumetDefaultVideoServer
            )
        }

        when (videoData) {
            is Resource.Failure -> emit(VideoDataDialogState.ERROR)
            Resource.Loading -> Unit
            is Resource.Success -> {
                val data = videoData.data
                if (data != null) {
                    emit(VideoDataDialogState.SUCCESS)
                    onSuccess(
                        data.copy(
                            title = titleToUse,
                            mediaId = mediaId
                        ),
                        episodeToUse
                    )
                } else {
                    emit(VideoDataDialogState.UNAVAILABLE)
                }
            }
        }
    }
}