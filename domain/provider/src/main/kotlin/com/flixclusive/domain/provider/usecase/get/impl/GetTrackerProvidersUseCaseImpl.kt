package com.flixclusive.domain.provider.usecase.get.impl

import android.content.Context
import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.data.provider.repository.ProviderResponseWrapper
import com.flixclusive.domain.provider.usecase.get.GetTrackerProvidersUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.transformLatest
import javax.inject.Inject

internal class GetTrackerProvidersUseCaseImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val providerRepository: ProviderRepository,
    private val userSessionDataStore: UserSessionDataStore,
) : GetTrackerProvidersUseCase {
    @OptIn(FlowPreview::class)
    override fun invoke(): Flow<Async<List<ProviderResponseWrapper>>> {
        return userSessionDataStore.currentUserId.filterNotNull().flatMapLatest { userId ->
            providerRepository
                .getProvidersAsFlow(userId)
                .distinctUntilChanged()
                .transformLatest { providers ->
                    if (providers.isEmpty()) {
                        emit(Async.Success(emptyList()))
                        return@transformLatest
                    }

                    emit(Async.Loading)
                    val metadata = providers
                        .filter { provider ->
                            provider.plugin?.getTrackerApi(context) != null
                        }

                    emit(Async.Success(metadata))
                }.catch { emit(Async.Failure(it)) }
        }
    }
}
