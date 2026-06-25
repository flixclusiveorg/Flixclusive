package com.flixclusive.domain.provider.usecase.get

import com.flixclusive.core.common.provider.LoadLinksState
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.common.tv.Episode
import kotlinx.coroutines.flow.Flow

/**
 * This use case is used to obtain the links of a media or episode
 * from a provider, which can be used to stream the media or episode.
 * */
interface GetMediaLinksUseCase {
    /**
     * Obtains the links of a media or episode using a cached watch ID.
     *
     * @param media The media or TV show to obtain the links for.
     * @param episode The episode of the TV show to obtain the links for, if applicable.
     * If the media is a movie, this can be null.
     *
     * @return A flow stream of [LoadLinksState]
     * */
    operator fun invoke(
        media: MediaMetadata,
        episode: Episode? = null,
    ): Flow<LoadLinksState>
}
