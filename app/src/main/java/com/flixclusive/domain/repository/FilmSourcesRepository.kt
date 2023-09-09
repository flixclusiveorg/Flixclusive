package com.flixclusive.domain.repository

import com.flixclusive.domain.common.Resource
import com.flixclusive.domain.model.tmdb.Film
import com.flixclusive.domain.model.tmdb.FilmType
import com.flixclusive_provider.models.common.VideoData

interface FilmSourcesRepository {
    suspend fun getStreamingLinks(
        mediaId: String,
        episodeId: String,
        server: String
    ): Resource<VideoData?>

    suspend fun getMediaId(film: Film?): String?

    suspend fun getEpisodeId(
        mediaId: String,
        filmType: FilmType,
        episode: Int? = null,
        season: Int? = null,
    ): String?
}