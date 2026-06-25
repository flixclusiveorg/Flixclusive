package com.flixclusive.domain.provider.usecase.tracker.impl

import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.domain.provider.usecase.tracker.GetTrackerApiUseCase
import com.flixclusive.domain.provider.usecase.tracker.GetTrackerListItemsUseCase
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.common.PaginatedMedia
import com.flixclusive.provider.tracker.TrackerList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

internal class GetTrackerListItemsUseCaseImpl @Inject constructor(
    private val getTrackerApi: GetTrackerApiUseCase
) : GetTrackerListItemsUseCase {
    override fun invoke(
        list: TrackerList,
        page: Int,
        pageSize: Int
    ): Flow<Async<PaginatedMedia<MediaMetadata>>> = flow {
        emit(Async.Loading)
        try {
            val api = getTrackerApi(list.providerId)

            val paginatedItems = api.getListItems(list, page, pageSize)

            emit(Async.Success(paginatedItems))
        } catch (e: Throwable) {
            errorLog(e)
            emit(Async.Failure(e))
        }
    }
}
