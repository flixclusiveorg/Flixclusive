package com.flixclusive.domain.provider.usecase.tracker.impl

import android.content.Context
import com.flixclusive.domain.provider.R
import com.flixclusive.domain.provider.usecase.get.GetProviderPluginUseCase
import com.flixclusive.domain.provider.usecase.tracker.GetTrackerApiUseCase
import com.flixclusive.provider.capability.TrackerFeature
import com.flixclusive.provider.capability.TrackerProviderApi
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

internal class GetTrackerApiUseCaseImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val getProviderPlugin: GetProviderPluginUseCase
) : GetTrackerApiUseCase {
    override suspend fun invoke(id: String): TrackerProviderApi {
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
}
