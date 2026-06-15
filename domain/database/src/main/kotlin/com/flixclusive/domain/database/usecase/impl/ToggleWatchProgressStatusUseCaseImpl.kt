package com.flixclusive.domain.database.usecase.impl

import com.flixclusive.core.database.entity.watched.EpisodeProgress
import com.flixclusive.core.database.entity.watched.MovieProgress
import com.flixclusive.core.database.entity.watched.WatchStatus
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.data.database.repository.WatchProgressRepository
import com.flixclusive.domain.database.usecase.ToggleWatchProgressStatusUseCase
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.Movie
import com.flixclusive.model.media.Show
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import javax.inject.Inject

internal class ToggleWatchProgressStatusUseCaseImpl @Inject constructor(
    private val watchProgressRepository: WatchProgressRepository,
    private val userSessionDataStore: UserSessionDataStore,
) : ToggleWatchProgressStatusUseCase {
    override suspend fun invoke(media: MediaMetadata) {
        val ownerId = userSessionDataStore.currentUserId.filterNotNull().first()

        when (media) {
            is Movie -> invokeForMovie(ownerId, media)
            is Show -> invokeForShow(ownerId, media)
        }
    }

    private suspend fun invokeForShow(ownerId: String, tvShow: Show) {
        val progress = watchProgressRepository.get(
            id = tvShow.id,
            type = tvShow.type,
            ownerId = ownerId,
        )

        if (progress == null) {
            val season = tvShow.totalSeasons
            val episode = tvShow.totalEpisodes

            watchProgressRepository.insert(
                media = tvShow,
                item = EpisodeProgress(
                    mediaId = tvShow.id,
                    ownerId = ownerId,
                    seasonNumber = season,
                    episodeNumber = episode,
                    status = WatchStatus.COMPLETED,
                    progress = 0L,
                ),
            )
        } else {
            for (i in 1..tvShow.totalSeasons) {
                val progressList = watchProgressRepository.getSeasonProgress(
                    tvShowId = tvShow.id,
                    ownerId = ownerId,
                    seasonNumber = i,
                )

                progressList.forEach { episodeProgress ->
                    watchProgressRepository.delete(
                        item = episodeProgress.id,
                        type = tvShow.type,
                    )
                }
            }
        }
    }

    private suspend fun invokeForMovie(ownerId: String, media: Movie) {
        val progress = watchProgressRepository.get(
            id = media.id,
            ownerId = ownerId,
            type = media.type,
        )

        if (progress == null) {
            watchProgressRepository.insert(
                media = media,
                item = MovieProgress(
                    mediaId = media.id,
                    ownerId = ownerId,
                    progress = 0L,
                    status = WatchStatus.COMPLETED,
                ),
            )
        } else {
            watchProgressRepository.delete(
                item = progress.id,
                type = media.type,
            )
        }
    }
}
