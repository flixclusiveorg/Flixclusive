package com.flixclusive.domain.provider.usecase.get.impl

import android.content.Context
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.core.util.exception.actualMessage
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.domain.provider.R
import com.flixclusive.domain.provider.usecase.get.GetFilmMetadataUseCase
import com.flixclusive.model.film.Film
import com.flixclusive.model.film.FilmMetadata
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

internal class GetFilmMetadataUseCaseImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val userSessionDataStore: UserSessionDataStore,
    private val providerRepository: ProviderRepository,
    private val appDispatchers: AppDispatchers
) : GetFilmMetadataUseCase {
    override operator fun invoke(film: Film): Flow<Async<FilmMetadata>> = flow {
        try {
            val userId = userSessionDataStore.currentUserId.filterNotNull().first()
            val provider = providerRepository.getProvider(
                id = film.providerId, ownerId = userId
            )

            if (provider == null) {
                emit(
                    Async.Failure(
                        UiText.from(
                            R.string.get_film_metadata_error_no_provider_plugin,
                            film.providerId
                        )
                    )
                )
                return@flow
            }

            val api = provider.plugin?.getMetadataApi(context)
            if (api == null) {
                emit(
                    Async.Failure(
                        UiText.from(R.string.get_film_metadata_error_no_provider_api, film.providerId)
                    )
                )
                return@flow
            }

            val metadata = api.getMetadata(film)

            emit(Async.Success(metadata))
        } catch (e: Exception) {
            errorLog(e)
            emit(
                Async.Failure(
                    UiText.from(R.string.get_film_metadata_error_unk_exception, e.actualMessage),
                )
            )
        }
    }.flowOn(appDispatchers.io)
}
