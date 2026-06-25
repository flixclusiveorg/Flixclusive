package com.flixclusive.domain.provider.usecase.get

import com.flixclusive.model.media.MediaMetadata

/**
 * Use case for fetching detailed metadata of a media from a cross-match provider.
 * */
interface GetCrossMatchedMediaMetadataUseCase {
    suspend operator fun invoke(media: MediaMetadata, providerId: String): MediaMetadata
}
