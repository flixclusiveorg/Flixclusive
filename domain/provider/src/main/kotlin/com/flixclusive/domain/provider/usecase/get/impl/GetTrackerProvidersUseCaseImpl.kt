package com.flixclusive.domain.provider.usecase.get.impl

import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.data.provider.ProviderCapability
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.data.provider.repository.ProviderResponseWrapper
import com.flixclusive.domain.provider.usecase.get.GetTrackerProvidersUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

internal class GetTrackerProvidersUseCaseImpl @Inject constructor(
    private val providerRepository: ProviderRepository,
    private val userSessionDataStore: UserSessionDataStore,
) : GetTrackerProvidersUseCase {
    override fun invoke(): Flow<Async<List<ProviderResponseWrapper>>> {
        return userSessionDataStore.currentUserId.filterNotNull().flatMapLatest { userId ->
            providerRepository
                .getProvidersWithCapabilityAsFlow(userId, ProviderCapability.TRACKER)
                .distinctUntilChanged()
                .map { Async.Success(it) as Async<List<ProviderResponseWrapper>> }
                .onStart { emit(Async.Loading) }
                .catch { emit(Async.Failure(it)) }
        }
    }
}
