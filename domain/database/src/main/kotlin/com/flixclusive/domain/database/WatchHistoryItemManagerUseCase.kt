package com.flixclusive.domain.database

import com.flixclusive.core.util.common.network.AppDispatchers
import com.flixclusive.core.util.common.network.Dispatcher
import com.flixclusive.core.util.database.isFinishedWatching
import com.flixclusive.data.watch_history.WatchHistoryRepository
import com.flixclusive.model.database.EpisodeWatched
import com.flixclusive.model.database.WatchHistoryItem
import com.flixclusive.model.tmdb.TMDBEpisode
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject

class WatchHistoryItemManagerUseCase @Inject constructor(
    private val watchHistoryRepository: WatchHistoryRepository,
    @Dispatcher(AppDispatchers.Default) private val defaultDispatcher: CoroutineDispatcher
) {
    suspend fun updateEpisodeCount(
        id: Int,
        seasonNumber: Int,
        episodeCount: Int,
    ): WatchHistoryItem? {
        val watchHistoryItem = watchHistoryRepository.getWatchHistoryItemById(id)
        var newWatchHistoryItem: WatchHistoryItem? = null

        watchHistoryItem?.let { item ->
            val newEpisodesMap = item.episodes.toMutableMap()
            newEpisodesMap[seasonNumber] = episodeCount

            newWatchHistoryItem = item.copy(episodes = newEpisodesMap)
            watchHistoryRepository.insert(newWatchHistoryItem!!)
        }

        return newWatchHistoryItem
    }

    suspend fun updateWatchHistoryItem(
        watchHistoryItem: WatchHistoryItem,
        currentTime: Long,
        totalDuration: Long,
        currentSelectedEpisode: TMDBEpisode?,
    ): WatchHistoryItem {
        val minute = 60000
        val isLessThanAMinute = currentTime <= minute

        if(isLessThanAMinute)
            return watchHistoryItem

        val newWatchHistoryItem: WatchHistoryItem
        val isTvShow = when (currentSelectedEpisode) {
            null -> false
            else -> true
        }

        if (isTvShow) {
            val updatedEpisodesWatchedList = watchHistoryItem.episodesWatched.toMutableList()

            val episodeWatchedIndex = withContext(defaultDispatcher) {
                watchHistoryItem.episodesWatched.indexOfFirst {
                    it.seasonNumber == currentSelectedEpisode!!.season
                            && it.episodeNumber == currentSelectedEpisode.episode
                }
            }

            val isEpisodeWatchedAlready = episodeWatchedIndex != -1
            if (isEpisodeWatchedAlready) {
                val episode = watchHistoryItem.episodesWatched[episodeWatchedIndex]
                updatedEpisodesWatchedList[episodeWatchedIndex] = episode.copy(
                    watchTime = currentTime,
                    durationTime = totalDuration,
                    isFinished = isFinishedWatching(
                        currentTime,
                        totalDuration
                    )
                )
            } else {
                val currentSelectedEpisodeId = currentSelectedEpisode!!.episodeId
                val episodeToAdd = EpisodeWatched(
                    episodeId = currentSelectedEpisodeId,
                    episodeNumber = currentSelectedEpisode.episode,
                    seasonNumber = currentSelectedEpisode.season,
                    watchTime = currentTime,
                    durationTime = totalDuration,
                    isFinished = isFinishedWatching(
                        currentTime,
                        totalDuration
                    )
                )

                val insertIndex = if(watchHistoryItem.episodesWatched.isEmpty()) {
                    0
                } else {
                    getSortedIndexOfItemInEpisodesWatched(
                        watchHistoryItem.episodesWatched,
                        episodeToAdd
                    )
                }

                updatedEpisodesWatchedList.add(insertIndex, episodeToAdd)
            }

            newWatchHistoryItem = watchHistoryItem.copy(
                episodesWatched = updatedEpisodesWatchedList,
                dateWatched = Date()
            )
        } else {
            val episodeWatchedItem = EpisodeWatched(
                episodeId = watchHistoryItem.id,
                watchTime = currentTime,
                durationTime = totalDuration,
                isFinished = isFinishedWatching(
                    currentTime,
                    totalDuration
                )
            )

            newWatchHistoryItem = watchHistoryItem.copy(
                episodesWatched = listOf(episodeWatchedItem),
                dateWatched = Date()
            )
        }

        watchHistoryRepository.insert(newWatchHistoryItem)

        return newWatchHistoryItem
    }

    private fun getSortedIndexOfItemInEpisodesWatched(
        episodes: List<EpisodeWatched>,
        episodeToAdd: EpisodeWatched
    ): Int {
        val insertIndex = episodes.binarySearch { watchedEpisode ->
            compareValuesBy(watchedEpisode, episodeToAdd, { it.seasonNumber }, { it.episodeNumber })
        }
        return when {
            insertIndex >= 0 -> insertIndex
            else -> -insertIndex - 1
        }
    }
}