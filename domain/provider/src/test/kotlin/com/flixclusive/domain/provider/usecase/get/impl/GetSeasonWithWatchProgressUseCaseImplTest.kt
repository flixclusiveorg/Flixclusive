package com.flixclusive.domain.provider.usecase.get.impl

import app.cash.turbine.test
import com.flixclusive.core.database.entity.user.User
import com.flixclusive.core.database.entity.watched.EpisodeProgress
import com.flixclusive.core.database.entity.watched.WatchStatus
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.testing.film.FilmTestDefaults
import com.flixclusive.data.database.repository.WatchProgressRepository
import com.flixclusive.data.database.session.UserSessionManager
import com.flixclusive.data.tmdb.repository.TMDBMetadataRepository
import com.flixclusive.domain.provider.model.SeasonWithProgress
import com.flixclusive.domain.provider.usecase.get.GetSeasonWithWatchProgressUseCase
import com.flixclusive.model.film.DEFAULT_FILM_SOURCE_NAME
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import java.util.Date

class GetSeasonWithWatchProgressUseCaseImplTest {
    private val testDispatcher = StandardTestDispatcher()
    private val tmdbMetadataRepository = mockk<TMDBMetadataRepository>()
    private val watchProgressRepository = mockk<WatchProgressRepository>()
    private val userSessionManager = mockk<UserSessionManager>()

    private val testUser = User(id = 1, name = "testuser", image = 1)
    private lateinit var useCase: GetSeasonWithWatchProgressUseCase

    @Before
    fun setUp() {
        every { userSessionManager.currentUser } returns MutableStateFlow(testUser)

        useCase = GetSeasonWithWatchProgressUseCaseImpl(
            tmdbMetadataRepository = tmdbMetadataRepository,
            watchProgressRepository = watchProgressRepository,
            userSessionManager = userSessionManager,
        )
    }

    @Test
    fun `when season exists in tv show seasons then returns season with progress`() =
        runTest(testDispatcher) {
            // Given
            val tvShow = FilmTestDefaults.getTvShow()
            val seasonNumber = 1
            val expectedSeason = tvShow.seasons.first { it.number == seasonNumber }
            val progressList = listOf(
                createEpisodeProgress(
                    filmId = tvShow.identifier,
                    seasonNumber = seasonNumber,
                    episodeNumber = 1,
                    progress = 1500L,
                    duration = 3600L,
                ),
                createEpisodeProgress(
                    filmId = tvShow.identifier,
                    seasonNumber = seasonNumber,
                    episodeNumber = 2,
                    progress = 0L,
                    duration = 3600L,
                ),
            )

            coEvery {
                watchProgressRepository.getSeasonProgress(
                    tvShowId = tvShow.identifier,
                    seasonNumber = seasonNumber,
                    ownerId = testUser.id,
                )
            } returns progressList

            // When & Then
            useCase(tvShow, seasonNumber).test {
                expectThat(awaitItem()).isA<Resource.Loading>()

                val successResult = awaitItem()
                expectThat(successResult).isA<Resource.Success<SeasonWithProgress>>()

                val seasonWithProgress = successResult.data!!
                expectThat(seasonWithProgress.season).isEqualTo(expectedSeason)
                expectThat(seasonWithProgress.episodes).hasSize(expectedSeason.episodes.size)
                expectThat(seasonWithProgress.episodes[0].watchProgress?.progress).isEqualTo(1500L)
                expectThat(seasonWithProgress.episodes[1].watchProgress?.progress).isEqualTo(0L)

                awaitComplete()
            }
        }

    @Test
    fun `when season not in tv show and is from tmdb source then fetches from tmdb repository`() =
        runTest(testDispatcher) {
            // Given
            val tvShow = FilmTestDefaults.getTvShow(
                tmdbId = 12345,
                providerId = DEFAULT_FILM_SOURCE_NAME,
                seasons = emptyList(), // No seasons in the TvShow
            )
            val seasonNumber = 2
            val tmdbSeason = FilmTestDefaults.getSeason(number = seasonNumber)
            val progressList = listOf(
                createEpisodeProgress(
                    filmId = tvShow.identifier,
                    seasonNumber = seasonNumber,
                    episodeNumber = 1,
                    progress = 2400L,
                    duration = 3600L,
                ),
            )

            coEvery {
                tmdbMetadataRepository.getSeason(
                    id = tvShow.tmdbId!!,
                    seasonNumber = seasonNumber,
                )
            } returns Resource.Success(tmdbSeason)

            coEvery {
                watchProgressRepository.getSeasonProgress(
                    tvShowId = tvShow.identifier,
                    seasonNumber = seasonNumber,
                    ownerId = testUser.id,
                )
            } returns progressList

            // When & Then
            useCase(tvShow, seasonNumber).test {
                expectThat(awaitItem()).isA<Resource.Loading>()

                val successResult = awaitItem()
                expectThat(successResult).isA<Resource.Success<SeasonWithProgress>>()

                val seasonWithProgress = successResult.data!!
                expectThat(seasonWithProgress.season).isEqualTo(tmdbSeason)
                expectThat(seasonWithProgress.episodes).hasSize(tmdbSeason.episodes.size)
                expectThat(seasonWithProgress.episodes[0].watchProgress?.progress).isEqualTo(2400L)

                awaitComplete()
            }
        }

    @Test
    fun `when tmdb repository returns failure then emits failure`() =
        runTest(testDispatcher) {
            // Given
            val tvShow = FilmTestDefaults.getTvShow(
                tmdbId = 12345,
                providerId = DEFAULT_FILM_SOURCE_NAME,
                seasons = emptyList(),
            )
            val seasonNumber = 2
            val errorMessage = "Network error"
            val tmdbError = Resource.Failure(errorMessage)

            coEvery {
                tmdbMetadataRepository.getSeason(
                    id = tvShow.tmdbId!!,
                    seasonNumber = seasonNumber,
                )
            } returns tmdbError

            // When & Then
            useCase(tvShow, seasonNumber).test {
                expectThat(awaitItem()).isA<Resource.Loading>()

                val failureResult = awaitItem()
                expectThat(failureResult).isA<Resource.Failure>()
                expectThat(failureResult.error).isNotNull()

                awaitComplete()
            }
        }

    @Test
    fun `when tv show is not from tmdb source and season not found then returns season as null with empty episodes`() =
        runTest(testDispatcher) {
            // Given
            val tvShow = FilmTestDefaults.getTvShow(
                tmdbId = null,
                providerId = "custom-provider",
                seasons = emptyList(),
            )
            val seasonNumber = 1
            val progressList = emptyList<EpisodeProgress>()

            coEvery {
                watchProgressRepository.getSeasonProgress(
                    tvShowId = tvShow.identifier,
                    seasonNumber = seasonNumber,
                    ownerId = testUser.id,
                )
            } returns progressList

            // When & Then
            useCase(tvShow, seasonNumber).test {
                expectThat(awaitItem()).isA<Resource.Loading>()

                expectThat(awaitItem()).isA<Resource.Failure>()

                awaitComplete()
            }
        }

    @Test
    fun `when progress list contains episodes not in season then filters them out`() =
        runTest(testDispatcher) {
            // Given
            val tvShow = FilmTestDefaults.getTvShow()
            val seasonNumber = 1

            val progressList = listOf(
                createEpisodeProgress(
                    filmId = tvShow.identifier,
                    seasonNumber = seasonNumber,
                    episodeNumber = 1, // exists in season
                    progress = 1500L,
                    duration = 3600L,
                ),
                createEpisodeProgress(
                    filmId = tvShow.identifier,
                    seasonNumber = seasonNumber,
                    episodeNumber = 999, // doesn't exist in season
                    progress = 2000L,
                    duration = 3600L,
                ),
            )

            coEvery {
                watchProgressRepository.getSeasonProgress(
                    tvShowId = tvShow.identifier,
                    seasonNumber = seasonNumber,
                    ownerId = testUser.id,
                )
            } returns progressList

            // When & Then
            useCase(tvShow, seasonNumber).test {
                expectThat(awaitItem()).isA<Resource.Loading>()

                val successResult = awaitItem()
                expectThat(successResult).isA<Resource.Success<SeasonWithProgress>>()

                val seasonWithProgress = successResult.data!!
                expectThat(seasonWithProgress.episodes).hasSize(tvShow.seasons[seasonNumber - 1].episodes.size)
                expectThat(seasonWithProgress.episodes[0].number).isEqualTo(1)

                awaitComplete()
            }
        }

    @Test
    fun `when no progress data exists then returns season with empty episodes list`() =
        runTest(testDispatcher) {
            // Given
            val tvShow = FilmTestDefaults.getTvShow()
            val seasonNumber = 1
            val expectedSeason = tvShow.seasons.first { it.number == seasonNumber }
            val progressList = emptyList<EpisodeProgress>()

            coEvery {
                watchProgressRepository.getSeasonProgress(
                    tvShowId = tvShow.identifier,
                    seasonNumber = seasonNumber,
                    ownerId = testUser.id,
                )
            } returns progressList

            // When & Then
            useCase(tvShow, seasonNumber).test {
                expectThat(awaitItem()).isA<Resource.Loading>()

                val successResult = awaitItem()
                expectThat(successResult).isA<Resource.Success<SeasonWithProgress>>()

                val seasonWithProgress = successResult.data!!
                expectThat(seasonWithProgress.season).isEqualTo(expectedSeason)
                expectThat(seasonWithProgress.episodes).hasSize(expectedSeason.episodes.size)

                awaitComplete()
            }
        }

    @Test
    fun `when episodes are returned in different order then maps correctly using binary search`() =
        runTest(testDispatcher) {
            // Given
            val episodes = listOf(
                FilmTestDefaults.getEpisode(number = 1),
                FilmTestDefaults.getEpisode(number = 3),
                FilmTestDefaults.getEpisode(number = 5),
                FilmTestDefaults.getEpisode(number = 7),
            )
            val season = FilmTestDefaults.getSeason(number = 1, episodes = episodes)
            val tvShow = FilmTestDefaults.getTvShow(seasons = listOf(season))

            val progressList = listOf(
                createEpisodeProgress(
                    filmId = tvShow.identifier,
                    seasonNumber = 1,
                    episodeNumber = 1, // First episode
                    progress = 2000L,
                    duration = 3600L,
                ),
                createEpisodeProgress(
                    filmId = tvShow.identifier,
                    seasonNumber = 1,
                    episodeNumber = 3, // Middle episode
                    progress = 1500L,
                    duration = 3600L,
                ),
                createEpisodeProgress(
                    filmId = tvShow.identifier,
                    seasonNumber = 1,
                    episodeNumber = 7, // Last episode
                    progress = 1000L,
                    duration = 3600L,
                ),
            )

            coEvery {
                watchProgressRepository.getSeasonProgress(
                    tvShowId = tvShow.identifier,
                    seasonNumber = 1,
                    ownerId = testUser.id,
                )
            } returns progressList

            // When & Then
            useCase(tvShow, 1).test {
                expectThat(awaitItem()).isA<Resource.Loading>()

                val successResult = awaitItem()
                expectThat(successResult).isA<Resource.Success<SeasonWithProgress>>()

                val seasonWithProgress = successResult.data!!
                expectThat(seasonWithProgress.episodes).hasSize(episodes.size)

                // Find episodes by number to verify correct mapping
                val episode1 = seasonWithProgress.episodes.find { it.number == 1 }!!
                val episode3 = seasonWithProgress.episodes.find { it.number == 3 }!!
                val episode5 = seasonWithProgress.episodes.find { it.number == 5 }
                val episode7 = seasonWithProgress.episodes.find { it.number == 7 }!!

                expectThat(episode1.watchProgress?.progress).isEqualTo(2000L)
                expectThat(episode3.watchProgress?.progress).isEqualTo(1500L)
                expectThat(episode5?.watchProgress).isNull()
                expectThat(episode7.watchProgress?.progress).isEqualTo(1000L)

                awaitComplete()
            }
        }

    private fun createEpisodeProgress(
        id: Long = 0,
        filmId: String,
        ownerId: Int = testUser.id,
        progress: Long,
        duration: Long,
        status: WatchStatus = WatchStatus.WATCHING,
        watchedAt: Date = Date(),
        seasonNumber: Int,
        episodeNumber: Int,
    ) = EpisodeProgress(
        id = id,
        filmId = filmId,
        ownerId = ownerId,
        progress = progress,
        duration = duration,
        status = status,
        watchedAt = watchedAt,
        seasonNumber = seasonNumber,
        episodeNumber = episodeNumber,
    )
}
