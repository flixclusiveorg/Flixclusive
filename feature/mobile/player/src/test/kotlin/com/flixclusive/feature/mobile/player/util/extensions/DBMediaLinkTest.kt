package com.flixclusive.feature.mobile.player.util.extensions

import com.flixclusive.core.database.entity.provider.CachedStream
import com.flixclusive.core.database.entity.provider.CachedSubtitle
import com.flixclusive.core.presentation.player.model.track.TrackSource
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.first
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo

class DBMediaLinkTest {
    private fun stream(
        url: String = "https://example.com/video.mp4",
        label: String = "1080p",
        isThirdPartyGateway: Boolean = false,
    ) = CachedStream(
        url = url,
        label = label,
        providerId = "provider",
        ownerId = "owner",
        mediaId = "media",
        isThirdPartyGateway = isThirdPartyGateway,
    )

    private fun subtitle(url: String = "https://example.com/sub.srt", label: String = "English") = CachedSubtitle(
        url = url,
        label = label,
        providerId = "provider",
        ownerId = "owner",
        mediaId = "media",
    )

    @Test
    fun `toPlayerServers should default to REMOTE source`() {
        val result = listOf(stream()).toPlayerServers()

        expectThat(result).first().get { source }.isEqualTo(TrackSource.REMOTE)
    }

    @Test
    fun `toPlayerServers should propagate an explicit source`() {
        val result = listOf(stream()).toPlayerServers(source = TrackSource.LOCAL)

        expectThat(result).first().get { source }.isEqualTo(TrackSource.LOCAL)
    }

    @Test
    fun `toPlayerServers should filter out third-party gateway streams`() {
        val result = listOf(stream(isThirdPartyGateway = true))

        expectThat(result.toPlayerServers()).hasSize(0)
    }

    @Test
    fun `toPlayerServer should default to REMOTE source`() {
        val result = stream().toPlayerServer()

        expectThat(result.source).isEqualTo(TrackSource.REMOTE)
    }

    @Test
    fun `toPlayerServer should propagate an explicit source`() {
        val result = stream().toPlayerServer(source = TrackSource.LOCAL)

        expectThat(result.source).isEqualTo(TrackSource.LOCAL)
    }

    @Test
    fun `toPlayerSubtitles should default to REMOTE source`() {
        val result = listOf(subtitle()).toPlayerSubtitles()

        expectThat(result).first().get { source }.isEqualTo(TrackSource.REMOTE)
    }

    @Test
    fun `toPlayerSubtitles should propagate an explicit source`() {
        val result = listOf(subtitle()).toPlayerSubtitles(source = TrackSource.LOCAL)

        expectThat(result).first().get { source }.isEqualTo(TrackSource.LOCAL)
    }

    @Test
    fun `toPlayerSubtitles should disambiguate duplicate labels`() {
        val result = listOf(
            subtitle(url = "https://example.com/sub1.srt", label = "English"),
            subtitle(url = "https://example.com/sub2.srt", label = "English"),
        ).toPlayerSubtitles()

        expectThat(result.map { it.label }).isEqualTo(listOf("English", "English 1"))
    }
}
