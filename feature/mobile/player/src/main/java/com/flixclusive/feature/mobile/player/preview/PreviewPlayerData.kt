package com.flixclusive.feature.mobile.player.preview

import com.flixclusive.core.presentation.player.model.track.PlayerServer
import com.flixclusive.core.presentation.player.model.track.PlayerSubtitle
import com.flixclusive.core.presentation.player.model.track.TrackSource

internal object PreviewPlayerData {
    fun getTestMediaServers(): List<PlayerServer> {
        val videos = listOf(
            "https://server15700.contentdm.oclc.org/dmwebservices/index.php?q=dmGetStreamingFile%2Fp15700coll2%2F15.mp4%2Fbyte%2Fjson",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackOnStreetAndDirt.mp4",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WhatCarCanYouGetForAGrand.mp4"
        )

        return videos.mapIndexed { index, url ->
            PlayerServer(
                label = "Server ${index + 1}",
                url = url,
                headers = null,
                source = TrackSource.REMOTE
            )
        }
    }

    fun getTestMediaSubtitles(): List<PlayerSubtitle> {
        return listOf(
            PlayerSubtitle(
                label = "Portuguese",
                url = "https://cdmdemo.contentdm.oclc.org/utils/getfile/collection/p15700coll2/id/18/filename/video2.vtt",
                source = TrackSource.REMOTE
            ),
            PlayerSubtitle(
                label = "English",
                url = "https://commons.wikimedia.org/w/api.php?action=timedtext&title=File%3ABig_Buck_Bunny_4K.webm&lang=en&trackformat=vtt",
                source = TrackSource.REMOTE
            ),
        )
    }
}
