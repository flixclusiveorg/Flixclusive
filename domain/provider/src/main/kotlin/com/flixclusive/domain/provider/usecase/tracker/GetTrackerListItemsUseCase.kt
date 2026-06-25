package com.flixclusive.domain.provider.usecase.tracker

import com.flixclusive.core.common.domain.Async
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.common.PaginatedMedia
import com.flixclusive.provider.tracker.TrackerList
import kotlinx.coroutines.flow.Flow

interface GetTrackerListItemsUseCase {
    operator fun invoke(
        list: TrackerList,
        page: Int = 1,
        pageSize: Int = 20
    ): Flow<Async<PaginatedMedia<MediaMetadata>>>
}
