package com.flixclusive.data.usecase

import com.flixclusive.di.DefaultDispatcher
import com.flixclusive.domain.model.entities.EpisodeWatched
import com.flixclusive.domain.model.entities.WatchHistoryItem
import com.flixclusive.domain.model.tmdb.TMDBEpisode
import com.flixclusive.domain.repository.WatchHistoryRepository
import com.flixclusive.domain.usecase.WatchHistoryItemManagerUseCase
import com.flixclusive.domain.utils.WatchHistoryUtils.isFinishedWatching
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject

class WatchHistoryItemManagerUseCaseImpl @Inject constructor(
    private val watchHistoryRepository: WatchHistoryRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : WatchHistoryItemManagerUseCase {
    override suspend fun updateEpisodeCount(
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

    override suspend fun updateWatchHistoryItem(
        watchHistoryItem: WatchHistoryItem,
        currentTime: Long,
        totalDuration: Long,
        currentSelectedEpisode: TMDBEpisode?,
    ): WatchHistoryItem {
        val currentTime = currentTime
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