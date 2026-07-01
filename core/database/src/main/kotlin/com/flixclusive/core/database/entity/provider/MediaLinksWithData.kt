package com.flixclusive.core.database.entity.provider

import com.flixclusive.core.database.entity.media.DBMedia
import com.flixclusive.core.database.entity.media.DBMediaExternalId

data class MediaLinksWithData(
    val media: DBMedia,
    val streams: List<CachedStream> = emptyList(),
    val subtitles: List<CachedSubtitle> = emptyList(),
    val externalIds: List<DBMediaExternalId> = emptyList(),
) {
    val providerId: String get() = media.providerId
    val ownerId: String? get() = streams.firstOrNull()?.ownerId ?: subtitles.firstOrNull()?.ownerId
    val episodeNumber: Int? get() = streams.firstOrNull()?.episodeNumber ?: subtitles.firstOrNull()?.episodeNumber
    val seasonNumber: Int? get() = streams.firstOrNull()?.seasonNumber ?: subtitles.firstOrNull()?.seasonNumber

    val size: Int get() = streams.size + subtitles.size
    val hasValidLinks: Boolean get() = streams.any { it.isValid }
}
