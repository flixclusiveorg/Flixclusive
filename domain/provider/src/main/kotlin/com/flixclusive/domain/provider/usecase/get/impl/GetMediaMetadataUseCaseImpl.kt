package com.flixclusive.domain.provider.usecase.get.impl

import android.content.Context
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.core.util.exception.actualMessage
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.domain.provider.R
import com.flixclusive.domain.provider.usecase.get.GetCrossMatchedMediaMetadataUseCase
import com.flixclusive.domain.provider.usecase.get.GetMediaMetadataUseCase
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.PartialMedia
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

internal class GetMediaMetadataUseCaseImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val userSessionDataStore: UserSessionDataStore,
    private val providerRepository: ProviderRepository,
    private val getCrossMatchedMediaMetadata: GetCrossMatchedMediaMetadataUseCase,
    private val appDispatchers: AppDispatchers,
) : GetMediaMetadataUseCase {
    override operator fun invoke(media: PartialMedia): Flow<Async<MediaMetadata>> = flow {
        try {
            val userId = userSessionDataStore.currentUserId.filterNotNull().first()
            val provider = providerRepository.getProvider(
                id = media.providerId,
                ownerId = userId
            )

            if (provider == null) {
                emit(
                    Async.Failure(
                        UiText.from(
                            R.string.get_media_metadata_error_no_provider_plugin,
                            media.providerId
                        )
                    )
                )
                return@flow
            }

            val api = provider.plugin?.getMetadataApi(context)
            if (api == null || !provider.isMetadataEnabled) {
                val providers = providerRepository.getProviders(ownerId = userId)

                providers
                    .filter { it.isCrossMatchEnabled && it.id != media.providerId }
                    .forEach {
                        val crossMatchedMedia = safeCall {
                            getCrossMatchedMediaMetadata(
                                media = media,
                                providerId = it.id
                            )
                        }

                        if (crossMatchedMedia is PartialMedia) {
                            emit(Async.Failure(UiText.from(R.string.get_media_metadata_error_unk_exception, it.id)))
                        }

                        if (crossMatchedMedia != null) {
                            emit(Async.Success(crossMatchedMedia))
                            return@flow
                        }
                    }

                emit(
                    Async.Failure(
                        UiText.from(R.string.get_media_metadata_error_no_provider_api, media.providerId)
                    )
                )
                return@flow
            }

            val metadata = when (media.isMovie) {
                true -> api.getMovie(media)
                false -> api.getShow(media)
            }

            emit(Async.Success(metadata))
        } catch (e: Throwable) {
            errorLog(e)
            emit(
                Async.Failure(
                    UiText.from(R.string.get_media_metadata_error_unk_exception, e.actualMessage),
                )
            )
        }
    }.flowOn(appDispatchers.io)
}
