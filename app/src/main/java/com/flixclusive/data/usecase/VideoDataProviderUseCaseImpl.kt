package com.flixclusive.data.usecase

import com.flixclusive.R
import com.flixclusive.common.UiText
import com.flixclusive.domain.common.Resource
import com.flixclusive.domain.model.VideoDataDialogState
import com.flixclusive.domain.model.entities.WatchHistoryItem
import com.flixclusive.domain.model.tmdb.Film
import com.flixclusive.domain.model.tmdb.TMDBEpisode
import com.flixclusive.domain.model.tmdb.TvShow
import com.flixclusive.domain.repository.ProvidersRepository
import com.flixclusive.domain.repository.TMDBRepository
import com.flixclusive.domain.repository.VideoDataSourceRepository
import com.flixclusive.domain.usecase.VideoDataProviderUseCase
import com.flixclusive.domain.utils.FilmProviderUtils.initializeSubtitles
import com.flixclusive.domain.utils.WatchHistoryUtils
import com.flixclusive.presentation.utils.FormatterUtils
import com.flixclusive.providers.models.common.VideoData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class VideoDataProviderUseCaseImpl @Inject constructor(
    private val videoDataSourceRepository: VideoDataSourceRepository,
    private val providersRepository: ProvidersRepository,
    private val tmdbRepository: TMDBRepository,
) : VideoDataProviderUseCase {
    override val providers: List<String>
        get() = providersRepository.providers.map { it.source.name }

    override fun invoke(
        film: Film,
        watchHistoryItem: WatchHistoryItem?,
        server: String?,
        source: String?,
        mediaId: String?,
        episode: TMDBEpisode?,
        onSuccess: (VideoData, TMDBEpisode?) -> Unit,
        onError: (() -> Unit)?,
    ): Flow<VideoDataDialogState> = flow {
        if(isThereNoAvailableSources()) {
            onError?.invoke()
            return@flow emit(VideoDataDialogState.Unavailable(R.string.no_available_sources))
        }

        for(i in providersRepository.providers.indices) {
            val provider = providersRepository.providers[i]

            val isSourceProvided = source != null && provider.source.name != source
            if(isSourceProvided || provider.isIgnored || provider.isMaintenance)
                continue

            emit(VideoDataDialogState.Fetching("Fetching from ${provider.source.name}..."))

            val canStopLooping = i == providersRepository.providers.lastIndex || source != null

            val id = mediaId ?: videoDataSourceRepository.getMediaId(
                film = film,
                providerIndex = i
            )

            if (id.isNullOrEmpty() && canStopLooping) {
                onError?.invoke()
                return@flow emit(VideoDataDialogState.Unavailable())
            } else if(id.isNullOrEmpty()) {
                continue
            }

            val isNewlyWatchShow =
                watchHistoryItem == null || watchHistoryItem.episodesWatched.isEmpty()

            var episodeToUse: TMDBEpisode? = episode
            if (episodeToUse == null && film is TvShow) {
                val seasonNumber: Int
                val episodeNumber: Int

                if (isNewlyWatchShow) {
                    if ((film.totalSeasons == 0 || film.totalEpisodes == 0) && canStopLooping) {
                        onError?.invoke()
                        return@flow emit(VideoDataDialogState.Unavailable())
                    }

                    seasonNumber = 1
                    episodeNumber = 1
                } else {
                    val lastEpisodeWatched =
                        WatchHistoryUtils.getNextEpisodeToWatch(watchHistoryItem!!)
                    seasonNumber = lastEpisodeWatched.first ?: 1
                    episodeNumber = lastEpisodeWatched.second ?: 1
                }

                val episodeFromApiService = tmdbRepository.getEpisode(
                    id = film.id,
                    seasonNumber = seasonNumber,
                    episodeNumber = episodeNumber
                )

                if (episodeFromApiService is Resource.Failure) {
                    if (canStopLooping) {
                        val errorMessage = episodeFromApiService.error
                            ?: UiText.StringResource(R.string.error_finding_episode_id_from_meta)

                        onError?.invoke()
                        return@flow emit(VideoDataDialogState.Error(errorMessage))
                    }

                    continue
                } else if (episodeFromApiService is Resource.Success && episodeFromApiService.data == null) {
                    if(canStopLooping) {
                        onError?.invoke()
                        return@flow emit(VideoDataDialogState.Unavailable(R.string.unavailable_episode))
                    }

                    continue
                }

                episodeToUse = episodeFromApiService.data
            }

            emit(VideoDataDialogState.Extracting("Extracting from ${provider.source.name}..."))
            val titleToUse = FormatterUtils.formatPlayerTitle(film, episodeToUse)

            val videoData = videoDataSourceRepository.getSourceLinks(
                mediaId = id,
                server = server,
                season = episodeToUse?.season,
                episode = episodeToUse?.episode,
                providerIndex = i
            )

            when (videoData) {
                is Resource.Failure -> {
                    if(canStopLooping) {
                        onError?.invoke()
                        return@flow emit(VideoDataDialogState.Error(videoData.error))
                    }
                }
                Resource.Loading -> Unit
                is Resource.Success -> {
                    val data = videoData.data
                    if (data != null) {
                        onSuccess(
                            data.copy(title = titleToUse)
                                .initializeSubtitles(),
                            episodeToUse
                        )
                        return@flow emit(VideoDataDialogState.Success)
                    } else if(canStopLooping) {
                        onError?.invoke()
                        return@flow emit(VideoDataDialogState.Unavailable())
                    }
                }
            }
        }

        emit(VideoDataDialogState.Unavailable())
    }

    private fun isThereNoAvailableSources() = providersRepository.providers.size == providersRepository.providers.filter { it.isIgnored || it.isMaintenance }.size
}