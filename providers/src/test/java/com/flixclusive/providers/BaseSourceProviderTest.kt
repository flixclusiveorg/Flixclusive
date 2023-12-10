package com.flixclusive.providers

import android.util.Base64
import com.flixclusive.providers.interfaces.SourceProvider
import com.flixclusive.providers.models.common.MediaInfo
import com.flixclusive.providers.models.common.MediaType
import com.flixclusive.providers.models.common.VideoData
import io.mockk.every
import io.mockk.mockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test

abstract class BaseSourceProviderTest {
    lateinit var sourceProvider: SourceProvider

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
        type: MediaType,
        season: Int? = null,
        episode: Int? = null,
    ): VideoData {
        val response = sourceProvider.search(
            query = title,
            page = 1,
            mediaType = type
        )

        val mediaId = response.results.find {
            val releaseDateToUse = if(it.releaseDate == null) {
                sourceProvider.getMediaInfo(it.id!!, it.mediaType!!).releaseDate
            } else it.releaseDate

            releaseDateToUse == releaseDate
        }?.id
        Assert.assertNotNull(mediaId)

        return sourceProvider.getSourceLinks(
            mediaId = mediaId!!,
            season = season,
            episode = episode
        )
    }

    private suspend fun getMediaInfo(
        title: String,
        releaseDate: String,
        type: MediaType,
    ): MediaInfo {
        val response = sourceProvider.search(
            query = title,
            page = 1,
            mediaType = type
        )

        val mediaId = response.results.find {
            val releaseDateToUse = if(it.releaseDate == null) {
                sourceProvider.getMediaInfo(it.id!!, it.mediaType!!).releaseDate
            } else it.releaseDate

            releaseDateToUse == releaseDate
        }?.id
        Assert.assertNotNull(mediaId)

        return sourceProvider.getMediaInfo(mediaId = mediaId!!, mediaType = type)
    }

    @Test
    open fun `Search for The Dark Knight (2008)`() = runTest {
        val title = "The Dark Knight"
        val releaseDate = "2008"
        val response = sourceProvider.search(
            query = title,
            page = 1,
            mediaType = MediaType.Movie
        )

        assert(response.results.isNotEmpty())
        assert(response.results.any { it.releaseDate == releaseDate })
    }

    @Test
    open fun `Get The Dark Knight (2008) source`() = runTest {
        val title = "The Dark Knight"
        val releaseDate = "2008"

        val data = getSourceData(
            title = title,
            releaseDate = releaseDate,
            type = MediaType.Movie
        )

        assert(data.subtitles.isNotEmpty())
        assert(data.source.isNotBlank())
        assert(data.servers?.isNotEmpty() == true)
    }

    @Test
    open fun `Get World War Z (2013) source`() = runTest {
        val title = "World War Z"
        val releaseDate = "2013"

        val data = getSourceData(
            title = title,
            releaseDate = releaseDate,
            type = MediaType.Movie
        )

        assert(data.subtitles.isNotEmpty())
        assert(data.source.isNotBlank())
        assert(data.servers?.isNotEmpty() == true)
    }

    @Test
    open fun `Get When Evil Lurks (2023) source`() = runTest {
        val title = "When Evil Lurks"
        val releaseDate = "2023"

        val data = getSourceData(
            title = title,
            releaseDate = releaseDate,
            type = MediaType.Movie
        )

        assert(data.subtitles.isNotEmpty())
        assert(data.source.isNotBlank())
        assert(data.servers?.isNotEmpty() == true)
    }

    @Test
    open fun `Get Silo (TV-2023) Details`() = runTest {
        val title = "Silo"
        val releaseDate = "2023"

        val data = getMediaInfo(
            title = title,
            releaseDate = releaseDate,
            type = MediaType.TvShow
        )

        Assert.assertEquals(data.title, title)
        assert(data.releaseDate.isNotBlank())
        assert(data.id.isNotEmpty())
    }

    @Test
    open fun `Get Attack on Titan (TV) S1-E3`() = runTest {
        val title = "Attack on Titan"
        val releaseDate = "2013"
        val season = 1
        val episode = 3

        val data = getSourceData(
            title = title,
            releaseDate = releaseDate,
            type = MediaType.TvShow,
            season = season,
            episode = episode
        )

        assert(data.subtitles.isNotEmpty())
        assert(data.source.isNotEmpty())
        assert(data.servers?.isNotEmpty() == true)
    }
}