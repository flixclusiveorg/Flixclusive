package com.flixclusive.domain.provider.usecase.tracker

import com.flixclusive.model.media.MediaMetadata
import kotlinx.coroutines.flow.Flow

interface GetTrackerListsForMediaUseCase {
    operator fun invoke(media: MediaMetadata): Flow<TrackerLists>
}
