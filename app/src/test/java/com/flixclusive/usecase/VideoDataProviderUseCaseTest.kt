package com.flixclusive.usecase

import com.flixclusive.TMDBAndFilmRepositoryBaseTest
import com.flixclusive.data.usecase.VideoDataProviderUseCaseImpl
import com.flixclusive.domain.model.VideoDataDialogState
import com.flixclusive.domain.model.entities.toWatchHistoryItem
import com.flixclusive.domain.model.tmdb.Season
import com.flixclusive.domain.model.tmdb.TMDBEpisode
import com.flixclusive.domain.usecase.VideoDataProviderUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class VideoDataProviderUseCaseTest : TMDBAndFilmRepositoryBaseTest() {
    private lateinit var videoDataProviderUseCase: VideoDataProviderUseCase

    @Before
    override fun setUp() {
        super.setUp()
        videoDataProviderUseCase = VideoDataProviderUseCaseImpl(filmSourcesRepository, tmdbRepository)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `provides the right video source data with FlixHQ`() = testScope.runTest {
        val flixHQSource = filmSourcesRepository.providers.last()

        val tvShow = tmdbRepository.getTvShow(sampleShowId)
        advanceUntilIdle()
        tvShow.assertDidNotFailAndNotNull()

        videoDataProviderUseCase(
            film = tvShow.data!!,
            source = flixHQSource.name,
            watchHistoryItem = tvShow.data?.toWatchHistoryItem(),
            server = null,
            onSuccess = { newData, _ ->
                assert(newData.source.isNotBlank())
            }
        ).collectLatest {
            println(it)
        }

    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `testing next episode functionality`() = testScope.runTest {
        val tvShow = tmdbRepository.getTvShow(sampleShowId)
        advanceUntilIdle()
        tvShow.assertDidNotFailAndNotNull()

        val season = tmdbRepository.getSeason(
            id = sampleShowId,
            seasonNumber = sampleShowSeason,
        )
        advanceUntilIdle()
        season.assertDidNotFailAndNotNull()

        val episode = tmdbRepository.getEpisode(
            id = sampleShowId,
            seasonNumber = sampleShowSeason,
            episodeNumber = sampleShowEpisode
        )
        advanceUntilIdle()
        episode.assertDidNotFailAndNotNull()

        val mediaId = filmSourcesRepository.getMediaId(tvShow.data)
        advanceUntilIdle()
        assertNotNull(mediaId)

        val watchHistoryItem = tvShow.data?.toWatchHistoryItem()
        val states = videoDataProviderUseCase(
            film = tvShow.data!!,
            mediaId = mediaId,
            watchHistoryItem = watchHistoryItem,
            episode = episode.data,
            server = null,
            onSuccess = { newData, _ ->
                println("First Episode: $newData")
                assertEquals(newData.mediaId, mediaId)
            }
        ).toList()
        assert(
            states.none {
                it is VideoDataDialogState.Unavailable
                    || it is VideoDataDialogState.Error
            }
        )

        val nextEpisode = episode.data!!.getNextEpisode(
            sampleShowId,
            tvShow.data!!.totalSeasons,
            season.data!!
        )
        assertNotNull(nextEpisode)
        assertEquals(sampleShowEpisode + 1, nextEpisode.episode)
        assertEquals(sampleShowSeason, nextEpisode.season)

        val nextEpisodeStates = videoDataProviderUseCase(
            film = tvShow.data!!,
            mediaId = mediaId,
            watchHistoryItem = watchHistoryItem,
            episode = episode.data,
            server = null,
            onSuccess = { newData, _ ->
                println("Second Episode: $newData")
                assertEquals(newData.mediaId, mediaId)
            }
        ).toList()
        assert(
            nextEpisodeStates.none {
                it is VideoDataDialogState.Unavailable
                    || it is VideoDataDialogState.Error
            }
        )
    }

    private suspend fun TMDBEpisode.getNextEpisode(showId: Int, seasonCount: Int, seasonData: Season): TMDBEpisode {
        val nextEpisode: TMDBEpisode?

        val episodesList = seasonData.episodes
        val nextEpisodeNumberToWatch = episode + 1

        if(episodesList.last().episode == nextEpisodeNumberToWatch)
            return episodesList.last()

        nextEpisode = if (episodesList.last().episode > nextEpisodeNumberToWatch) {
            episodesList.find {
                it.episode == nextEpisodeNumberToWatch
            } ?: throw NullPointerException("Episode cannot be null!")
        } else if (seasonData.seasonNumber + 1 <= seasonCount) {
            val newSeason = tmdbRepository.getSeason(
                id = showId,
                seasonNumber = seasonData.seasonNumber + 1,
            )
            newSeason.assertDidNotFailAndNotNull()

            newSeason.data!!.episodes[0]
        } else throw NullPointerException("Episode cannot be null!")

        return nextEpisode
    }
}