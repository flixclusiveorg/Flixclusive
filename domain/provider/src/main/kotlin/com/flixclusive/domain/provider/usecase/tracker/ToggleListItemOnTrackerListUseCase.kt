package com.flixclusive.domain.provider.usecase.tracker

import com.flixclusive.model.film.FilmMetadata
import com.flixclusive.provider.tracker.TrackerList

enum class TrackerListItemToggleAction {
    ADD,
    REMOVE
}

interface ToggleListItemOnTrackerListUseCase {
    suspend operator fun invoke(
        list: TrackerList,
        item: FilmMetadata,
        action: TrackerListItemToggleAction
    ): Result<Unit>
}
