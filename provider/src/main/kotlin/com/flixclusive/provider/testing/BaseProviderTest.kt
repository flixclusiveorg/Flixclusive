package com.flixclusive.provider.testing

import android.util.Base64
import com.flixclusive.core.util.film.FilmType
import com.flixclusive.model.provider.SourceLink
import com.flixclusive.model.provider.Subtitle
import com.flixclusive.provider.ProviderApi
import com.flixclusive.provider.dto.FilmInfo
import io.mockk.every
import io.mockk.mockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test

abstract class BaseProviderTest {
    lateinit var sourceProviderApi: ProviderApi

    @Before
    open fun setUp(): Unit = runTest {
        mockkStatic(Base64::class)
        every { Base64.encodeToString(any(), any()) } answers {
            val byteArray = arg<ByteArray>(0)
            java.util.Base64.getEncoder().encodeToString(byteArray)
        }
        every { Base64.encode(any(), any()) } answers {
            val byteArray = arg<String>(0).toByteArray()
            java.util.Base64.getEncoder().encode(byteArray)
        }
        every { Base64.decode(any<String>(), any()) } answers {
            val byteArray = arg<String>(0).toByteArray()
            java.util.Base64.getDecoder().decode(byteArray)
        }
        every { Base64.decode(any<ByteArray>(), any()) } answers {
            java.util.Base64.getDecoder().decode(arg<ByteArray>(0))
        }
    }

    private suspend fun getSourceData(
        title: String,
        releaseDate: String,
        type: FilmType,
        season: Int? = null,
        episode: Int? = null,
    ) {
        val response = sourceProviderApi.search(
            query = title,
            page = 1,
            filmType = type
        )

        val filmId = response.results.find {
            val releaseDateToUse = if(it.releaseDate == null) {
                sourceProviderApi.getFilmInfo(
                    filmId = it.id!!,
                    filmType = it.filmType!!
                ).yearReleased
            } else it.releaseDate

            releaseDateToUse == releaseDate
        }?.id
        Assert.assertNotNull(filmId)

        val links = mutableSetOf<SourceLink>()
        val subtitles = mutableSetOf<Subtitle>()
        sourceProviderApi.getSourceLinks(
            filmId = filmId!!,
            season = season,
            episode = episode,
            onLinkLoaded = {
                assert(it.url.isNotEmpty())
                links.add(it)
                println("SourceLink for $title: $it")
            },
            onSubtitleLoaded = {
                assert(it.url.isNotEmpty())
                subtitles.add(it)
                println("Subtitle for $title: $it")
            }
        )

        assert(links.isNotEmpty())
        assert(subtitles.isNotEmpty())
    }

    private suspend fun getFilmInfo(
        title: String,
        releaseDate: String,
        type: FilmType,
    ): FilmInfo {
        val response = sourceProviderApi.search(
            query = title,
            page = 1,
            filmType = type
        )

        val filmId = response.results.find {
            val releaseDateToUse = if(it.releaseDate == null) {
                sourceProviderApi.getFilmInfo(
                    filmId = it.id!!,
                    filmType = it.filmType!!
                ).yearReleased
            } else it.releaseDate

            releaseDateToUse == releaseDate
        }?.id
        Assert.assertNotNull(filmId)

        return sourceProviderApi.getFilmInfo(
            filmId = filmId!!,
            filmType = type
        )
    }

    @Test
    open fun search_for_The_Dark_Knight_2008() = runTest {
        val title = "The Dark Knight"
        val releaseDate = "2008"
        val response = sourceProviderApi.search(
            query = title,
            page = 1,
            filmType = FilmType.MOVIE
        )

        assert(response.results.isNotEmpty())
        assert(response.results.any { it.releaseDate == releaseDate })
    }

    @Test
    open fun get_source_for_The_Dark_Knight_2008() = runTest {
        val title = "The Dark Knight"
        val releaseDate = "2008"

        getSourceData(
            title = title,
            releaseDate = releaseDate,
            type = FilmType.MOVIE
        )
    }

    @Test
    open fun getfor_World_War_Z_2013() = runTest {
        val title = "World War Z"
        val releaseDate = "2013"

        getSourceData(
            title = title,
            releaseDate = releaseDate,
            type = FilmType.MOVIE
        )
    }

    @Test
    open fun get_for_When_Evil_Returns_2023() = runTest {
        val title = "When Evil Lurks"
        val releaseDate = "2023"

        getSourceData(
            title = title,
            releaseDate = releaseDate,
            type = FilmType.MOVIE
        )
    }

    @Test
    open fun get_for_Silo_2023_tv_show() = runTest {
        val title = "Silo"
        val releaseDate = "2023"

        val data = getFilmInfo(
            title = title,
            releaseDate = releaseDate,
            type = FilmType.TV_SHOW
        )

        Assert.assertEquals(data.title, title)
        assert(data.yearReleased.isNotBlank())
        assert(data.id.isNotEmpty())
    }

    @Test
    open fun get_for_Attack_On_Titan_2013_tv_show_S1_E3() = runTest {
        val title = "Attack on Titan"
        val releaseDate = "2013"
        val season = 1
        val episode = 3

        getSourceData(
            title = title,
            releaseDate = releaseDate,
            type = FilmType.TV_SHOW,
            season = season,
            episode = episode
        )
    }
}