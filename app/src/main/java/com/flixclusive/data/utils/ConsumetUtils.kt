package com.flixclusive.data.utils

import com.flixclusive.data.dto.consumet.ConsumetEpisode
import com.flixclusive.domain.model.tmdb.TMDBEpisode

object ConsumetUtils {
    fun getConsumetEpisodeId(
        episode: TMDBEpisode,
        listOfEpisode: List<ConsumetEpisode>
    ): String? {
        var episodeId = listOfEpisode
            .find { it.title.contains(episode.title, ignoreCase = true) }?.id

        if(episodeId == null) {
            episodeId = listOfEpisode
                .find {
                    it.number == episode.episode && episode.season == it.season
                }?.id
        }

        return episodeId
    }
}