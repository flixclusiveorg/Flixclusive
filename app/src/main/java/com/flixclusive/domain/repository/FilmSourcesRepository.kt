package com.flixclusive.domain.repository

import androidx.compose.runtime.snapshots.SnapshotStateList
import com.flixclusive.domain.common.Resource
import com.flixclusive.domain.model.provider.SourceProviderDetails
import com.flixclusive.domain.model.tmdb.Film
import com.flixclusive.providers.models.common.VideoData

interface FilmSourcesRepository {
    val providers: SnapshotStateList<SourceProviderDetails>

    suspend fun getSourceLinks(
        mediaId: String,
        providerIndex: Int = 0,
        server: String? = null,
        season: Int? = null,
        episode: Int? = null,
    ): Resource<VideoData?>

    suspend fun getMediaId(
        film: Film?,
        providerIndex: Int = 0,
    ): String?
}