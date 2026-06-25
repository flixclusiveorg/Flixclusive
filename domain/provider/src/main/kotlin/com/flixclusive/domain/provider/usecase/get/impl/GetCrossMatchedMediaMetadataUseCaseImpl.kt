package com.flixclusive.domain.provider.usecase.get.impl

import android.content.Context
import com.flixclusive.domain.provider.R
import com.flixclusive.domain.provider.usecase.get.GetCrossMatchedMediaMetadataUseCase
import com.flixclusive.domain.provider.usecase.get.GetInstalledProviderUseCase
import com.flixclusive.domain.provider.usecase.get.GetProviderPluginUseCase
import com.flixclusive.model.media.MediaMetadata
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

internal class GetCrossMatchedMediaMetadataUseCaseImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val getProviderPlugin: GetProviderPluginUseCase,
    private val getInstalledProvider: GetInstalledProviderUseCase,
) : GetCrossMatchedMediaMetadataUseCase {
    override suspend fun invoke(
        media: MediaMetadata,
        providerId: String
    ): MediaMetadata {
        if (media.providerId == providerId) return media

        val plugin = getProviderPlugin(providerId)
            ?: error(context.getString(R.string.cross_match_failed_to_find_plugin, providerId))

        val installedProvider = getInstalledProvider(providerId)
        if (installedProvider == null || !installedProvider.isCrossMatchEnabled) {
            error(context.getString(R.string.cross_match_not_allowed, providerId))
        }

        val api = plugin.getCrossMatchApi(context)
            ?: error(context.getString(R.string.cross_match_not_allowed, providerId))

        return api.getById(
            mediaType = media.type,
            sourceIds = media.externalIds,
        ) ?: api.getByFuzzy(media)
            ?: error(context.getString(R.string.cross_match_no_item_found, media.title, providerId))
    }
}
