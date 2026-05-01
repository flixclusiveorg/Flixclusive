package com.flixclusive.domain.provider.usecase.tracker.impl

import android.content.Context
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.domain.provider.R
import com.flixclusive.domain.provider.usecase.get.GetProviderPluginUseCase
import com.flixclusive.domain.provider.usecase.tracker.ToggleListItemOnTrackerListUseCase
import com.flixclusive.domain.provider.usecase.tracker.TrackerListItemToggleAction
import com.flixclusive.model.film.FilmIdSource
import com.flixclusive.model.film.FilmMetadata
import com.flixclusive.provider.capability.CrossMatchProviderApi
import com.flixclusive.provider.capability.TrackerFeature
import com.flixclusive.provider.capability.TrackerProviderApi
import com.flixclusive.provider.tracker.TrackerList
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

internal class ToggleListItemOnTrackerListUseCaseImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val getProviderPlugin: GetProviderPluginUseCase,
) : ToggleListItemOnTrackerListUseCase {
    override suspend fun invoke(
        list: TrackerList,
        item: FilmMetadata,
        action: TrackerListItemToggleAction
    ): Result<Unit> {
        return runCatching {
            val trackerProviderId = list.providerId
            val metadataProviderId = item.providerId

            val trackerApi = getTrackerApi(trackerProviderId)

            if (trackerProviderId != metadataProviderId) {
                val crossMatchApi = getCrossMatchApi(trackerProviderId, item.externalIds)

                val matchedItem = safeCall {
                    crossMatchApi.getById(item.externalIds)
                        ?: crossMatchApi.getByFuzzy(item)
                } ?: error(context.getString(R.string.tracker_no_matching_id_for_item))

                return@runCatching if (action == TrackerListItemToggleAction.ADD) {
                    trackerApi.addListItem(list, matchedItem)
                } else {
                    trackerApi.removeListItem(list, matchedItem)
                }
            }

            if (action == TrackerListItemToggleAction.ADD) {
                trackerApi.addListItem(list, item)
            } else {
                trackerApi.removeListItem(list, item)
            }
        }
    }

    private suspend fun getTrackerApi(id: String): TrackerProviderApi {
        val plugin = getProviderPlugin(id)
            ?: error(context.getString(R.string.tracker_failed_to_add_item_to_list_from_tracker))

        val api = plugin.getTrackerApi(context)
            ?: error(context.getString(R.string.tracker_failed_to_add_item_to_list_from_tracker))

        if (!api.getFeatures().contains(TrackerFeature.LIST_MANAGEMENT)) {
            error(context.getString(R.string.tracker_no_list_management_feature, plugin.name))
        }

        if (!api.isAuthenticated()) {
            error("${plugin.name} requires authentication to perform this action.")
        }

        return api
    }

    private suspend fun getCrossMatchApi(
        trackerId: String,
        externalIds: Map<FilmIdSource, String>
    ): CrossMatchProviderApi {
        val plugin = getProviderPlugin(trackerId)
            ?: error(context.getString(R.string.tracker_failed_to_add_item_to_list_from_tracker))

        val api = plugin.getCrossMatchApi(context)
            ?: error(context.getString(R.string.tracker_failed_to_add_item_to_list_from_tracker))

        if (!api.supportedIdSources.intersect(externalIds.keys).any()) {
            error("The provider ${plugin.name} doesn't support any of the provided ID sources of this item.")
        }

        return api
    }
}
