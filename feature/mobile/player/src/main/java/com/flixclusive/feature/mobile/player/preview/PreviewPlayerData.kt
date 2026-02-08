package com.flixclusive.feature.mobile.player.preview

import com.flixclusive.core.presentation.player.model.track.MediaServer
import com.flixclusive.core.presentation.player.model.track.MediaSubtitle
import com.flixclusive.core.presentation.player.model.track.TrackSource

internal object PreviewPlayerData {
    fun getTestMediaServers(): List<MediaServer> {
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
            MediaServer(
                label = "Server ${index + 1}",
                url = url,
                headers = null,
                source = TrackSource.REMOTE
            )
        }
    }

    fun getTestMediaSubtitles(): List<MediaSubtitle> {
        return listOf(
            MediaSubtitle(
                label = "Portuguese",
                url = "https://cdmdemo.contentdm.oclc.org/utils/getfile/collection/p15700coll2/id/18/filename/video2.vtt",
                source = TrackSource.REMOTE
            ),
            MediaSubtitle(
                label = "English",
                url = "https://cdmdemo.contentdm.oclc.org/utils/getfile/collection/p15700coll2/id/18/filename/video2.vtt",
                source = TrackSource.REMOTE
            ),
            MediaSubtitle(
                label = "French",
                url = "https://cdmdemo.contentdm.oclc.org/utils/getfile/collection/p15700coll2/id/18/filename/video2.vtt",
                source = TrackSource.REMOTE
            )
        )
    }
}
