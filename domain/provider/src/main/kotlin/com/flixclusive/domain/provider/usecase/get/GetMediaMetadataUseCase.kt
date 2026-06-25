package com.flixclusive.domain.provider.usecase.get

import com.flixclusive.core.common.domain.Async
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.PartialMedia
import kotlinx.coroutines.flow.Flow

/**
 * Use case for fetching detailed metadata of a media.
 * */
interface GetMediaMetadataUseCase {
    /**
     * Fetches detailed metadata for a given [media].
     *
     * The method first checks if the media's provider is not the default source. If it's a custom provider,
     * it attempts to fetch metadata using the corresponding provider API. If the provider is the default source, it will fetch from TMDB.
     *
     * @param media The media for which metadata is to be fetched.
     * @return A [Async] containing [MediaMetadata] on success or an error message
     * */
    operator fun invoke(media: PartialMedia): Flow<Async<MediaMetadata>>
}
