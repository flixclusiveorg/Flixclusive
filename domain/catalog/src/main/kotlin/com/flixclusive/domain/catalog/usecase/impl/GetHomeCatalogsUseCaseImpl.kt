package com.flixclusive.domain.catalog.usecase.impl

import android.content.Context
import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.data.provider.ProviderCapability
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.domain.catalog.usecase.GetHomeCatalogsUseCase
import com.flixclusive.model.provider.Catalog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.transformLatest
import javax.inject.Inject

internal class GetHomeCatalogsUseCaseImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val userSessionDataStore: UserSessionDataStore,
    private val providerRepository: ProviderRepository,
) : GetHomeCatalogsUseCase {
    override operator fun invoke(): Flow<Async<List<Catalog>>> {
        return userSessionDataStore.currentUserId.filterNotNull().flatMapLatest { userId ->
            providerRepository
                .getProvidersWithCapabilityAsFlow(
                    ownerId = userId,
                    capability = ProviderCapability.CATALOG
                ).transformLatest { providers ->
                    if (providers.isEmpty()) {
                        emit(Async.Success(emptyList()))
                        return@transformLatest
                    }

                    emit(Async.Loading)

                    val apis = providers
                        .mapNotNull { provider ->
                            if (!provider.isCatalogEnabled) return@mapNotNull null
                            provider.plugin?.getCatalogApi(context)
                        }

                    val catalogs = apis.flatMap { it.getCatalogs() }
                    emit(Async.Success(catalogs.shuffled()))
                }.catch { e ->
                    emit(Async.Failure(e))
                }
        }
    }
}
