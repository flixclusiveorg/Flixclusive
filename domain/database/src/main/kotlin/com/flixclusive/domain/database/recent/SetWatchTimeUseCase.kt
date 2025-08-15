package com.flixclusive.domain.database.recent

import com.flixclusive.core.database.entity.EpisodeWatched
import com.flixclusive.core.database.entity.WatchHistory
import com.flixclusive.core.database.entity.util.isFinishedWatching
import com.flixclusive.data.database.repository.WatchHistoryRepository
import com.flixclusive.model.film.common.tv.Episode
import com.flixclusive.model.film.util.FilmType
import java.util.Date
import javax.inject.Inject

class SetWatchTimeUseCase
    @Inject
    constructor(
        private val watchHistoryRepository: WatchHistoryRepository,
    ) {
        /**
         * Sets the watch time for a [WatchHistory] based on the current time and total duration.
         *
         * @param watchHistory The [WatchHistory] to update.
         * @param currentTime The current watch time in milliseconds.
         * @param totalDuration The total duration of the media in milliseconds.
         * @param currentSelectedEpisode The currently selected [Episode] if applicable, or null for movies.
         *
         * @return The updated [WatchHistory] with the new watch time set.
         * */
        suspend operator fun invoke(
            watchHistory: WatchHistory,
            currentTime: Long,
            totalDuration: Long,
            currentSelectedEpisode: Episode?,
        ): WatchHistory {
            if (isLessThanAMinute(currentTime)) {
                return watchHistory
            }

            val newWatchHistory: WatchHistory
            val isTvShow = watchHistory.film.filmType == FilmType.TV_SHOW

            if (isTvShow) {
                val newWatchedList = watchHistory.episodesWatched.toMutableList()

                val episodeWatchedIndex =
                    watchHistory.episodesWatched.indexOfFirst {
                        it.seasonNumber == currentSelectedEpisode!!.season &&
                            it.episodeNumber == currentSelectedEpisode.number
                    }

                val isEpisodeWatchedAlready = episodeWatchedIndex != -1
                if (isEpisodeWatchedAlready) {
                    val episode = watchHistory.episodesWatched[episodeWatchedIndex]
                    newWatchedList[episodeWatchedIndex] =
                        episode.copy(
                            watchTime = currentTime,
                            durationTime = totalDuration,
                            isFinished =
                                isFinishedWatching(
                                    currentTime,
                                    totalDuration,
                                ),
                        )
                } else {
                    val currentSelectedEpisodeId = currentSelectedEpisode!!.id
                    val episodeToAdd =
                        EpisodeWatched(
                            episodeId = currentSelectedEpisodeId,
                            episodeNumber = currentSelectedEpisode.number,
                            seasonNumber = currentSelectedEpisode.season,
                            watchTime = currentTime,
                            durationTime = totalDuration,
                            isFinished =
                                isFinishedWatching(
                                    currentTime,
                                    totalDuration,
                                ),
                        )

                    val insertIndex =
                        if (watchHistory.episodesWatched.isEmpty()) {
                            0
                        } else {
                            getSortedIndexOfItemInEpisodesWatched(
                                watchHistory.episodesWatched,
                                episodeToAdd,
                            )
                        }

                    newWatchedList.add(insertIndex, episodeToAdd)
                }

                newWatchHistory =
                    watchHistory.copy(
                        episodesWatched = newWatchedList,
                        dateWatched = Date(),
                    )
            } else {
                val episodeWatchedItem =
                    EpisodeWatched(
                        episodeId = watchHistory.id,
                        watchTime = currentTime,
                        durationTime = totalDuration,
                        isFinished =
                            isFinishedWatching(
                                currentTime,
                                totalDuration,
                            ),
                    )

                newWatchHistory =
                    watchHistory.copy(
                        episodesWatched = listOf(episodeWatchedItem),
                        dateWatched = Date(),
                    )
            }

            watchHistoryRepository.insert(newWatchHistory)

            return newWatchHistory
        }

        private fun isLessThanAMinute(currentTime: Long): Boolean {
            val minute = 60000
            return currentTime <= minute
        }

        private fun getSortedIndexOfItemInEpisodesWatched(
            episodes: List<EpisodeWatched>,
            episodeToAdd: EpisodeWatched,
        ): Int {
            val insertIndex =
                episodes.binarySearch { watchedEpisode ->
                    compareValuesBy(
                        watchedEpisode,
                        episodeToAdd,
                        { it.seasonNumber },
                        { it.episodeNumber },
                    )
                }
            return when {
                insertIndex >= 0 -> insertIndex
                else -> -insertIndex - 1
            }
        }
    }
