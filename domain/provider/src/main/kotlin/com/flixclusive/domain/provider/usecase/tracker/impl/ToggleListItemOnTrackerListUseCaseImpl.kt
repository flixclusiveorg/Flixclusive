package com.flixclusive.domain.provider.usecase.tracker.impl

import android.content.Context
import com.flixclusive.domain.provider.R
import com.flixclusive.domain.provider.usecase.tracker.GetTrackerApiUseCase
import com.flixclusive.domain.provider.usecase.tracker.ToggleListItemOnTrackerListUseCase
import com.flixclusive.domain.provider.usecase.tracker.TrackerListItemToggleAction
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.provider.tracker.TrackerList
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

internal class ToggleListItemOnTrackerListUseCaseImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val getTrackerApi: GetTrackerApiUseCase,
) : ToggleListItemOnTrackerListUseCase {
    override suspend fun invoke(
        list: TrackerList,
        item: MediaMetadata,
        action: TrackerListItemToggleAction
    ): Result<TrackerList> {
        return runCatching {
            require(item.providerId == list.providerId) {
                context.getString(R.string.tracker_not_matched_provider)
            }

            val trackerApi = getTrackerApi(list.providerId)

            if (action == TrackerListItemToggleAction.ADD) {
                trackerApi.addListItem(list, item)
            } else {
                trackerApi.removeListItem(list, item)
            }

            trackerApi.getList(list.id)
        }
    }
}
