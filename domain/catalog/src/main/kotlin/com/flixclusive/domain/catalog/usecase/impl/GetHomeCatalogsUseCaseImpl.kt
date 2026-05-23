package com.flixclusive.domain.catalog.usecase.impl

import android.content.Context
import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.data.provider.ProviderCapability
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.domain.catalog.usecase.GetHomeCatalogsUseCase
import com.flixclusive.model.provider.Catalog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

internal class GetHomeCatalogsUseCaseImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val userSessionDataStore: UserSessionDataStore,
    private val providerRepository: ProviderRepository,
) : GetHomeCatalogsUseCase {
    @OptIn(FlowPreview::class)
    private fun getCatalogsFlow(userId: String) =
        providerRepository.getProvidersWithCapabilityAsFlow(userId, ProviderCapability.CATALOG)
            .debounce(600) // Debounce to prevent rapid emissions when providers change
            .mapLatest { providers ->
                val apis = providers
                    .mapNotNull { provider ->
                        if (!provider.isCatalogEnabled) return@mapNotNull null
                        provider.plugin?.getCatalogApi(context)
                    }

                apis.flatMap { it.getCatalogs() }
            }

    override operator fun invoke(): Flow<Async<List<Catalog>>> {
        return userSessionDataStore.currentUserId.filterNotNull().flatMapLatest { userId ->
            getCatalogsFlow(userId)
                .mapLatest { catalogs ->
                    val list = catalogs
                        .distinctBy { it.name }
                        .shuffled()

                    Async.Success(list) as Async<List<Catalog>>
                }
                .onStart { emit(Async.Loading) }
                .catch { e ->
                    emit(Async.Failure(e))
                }
        }
    }
}
