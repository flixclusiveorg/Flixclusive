package com.flixclusive.domain.provider.usecase.tracker

import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.provider.tracker.TrackerList

enum class TrackerListItemToggleAction {
    ADD,
    REMOVE
}

interface ToggleListItemOnTrackerListUseCase {
    suspend operator fun invoke(
        list: TrackerList,
        item: MediaMetadata,
        action: TrackerListItemToggleAction
    ): Result<TrackerList>
}
