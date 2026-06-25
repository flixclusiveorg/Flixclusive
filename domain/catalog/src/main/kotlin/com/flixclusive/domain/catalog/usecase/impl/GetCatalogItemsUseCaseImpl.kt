package com.flixclusive.domain.catalog.usecase.impl

import android.content.Context
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.core.util.exception.actualMessage
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.core.util.log.warnLog
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.domain.catalog.R
import com.flixclusive.domain.catalog.usecase.GetCatalogItemsUseCase
import com.flixclusive.model.media.PartialMedia
import com.flixclusive.model.media.common.PaginatedMedia
import com.flixclusive.model.provider.Catalog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class GetCatalogItemsUseCaseImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val userSessionDataStore: UserSessionDataStore,
    private val providerRepository: ProviderRepository,
    private val appDispatchers: AppDispatchers,
) : GetCatalogItemsUseCase {
    override operator fun invoke(
        catalog: Catalog,
        page: Int,
    ): Flow<Async<PaginatedMedia<PartialMedia>>> = flow {
        try {
            val userId = userSessionDataStore.currentUserId.filterNotNull().first()
            val provider = providerRepository.getProvider(
                id = catalog.providerId,
                ownerId = userId
            )

            if (provider == null) {
                warnLog(
                    "Failed to get catalog items: Provider with id ${catalog.providerId} not found for user $userId"
                )
                emit(
                    Async.Failure(
                        UiText.from(
                            R.string.get_catalog_items_error_no_provider_plugin,
                            catalog.providerId,
                        ),
                    )
                )
                return@flow
            }

            val api = provider.plugin?.getCatalogApi(context)
            if (api == null) {
                warnLog(
                    "Failed to get catalog items: Provider has catalogs but has no CatalogProviderApi implementation for provider ${provider.id}"
                )
                emit(
                    Async.Failure(
                        UiText.from(
                            R.string.get_catalog_items_error_no_provider_api,
                            catalog.providerId,
                        ),
                    )
                )
                return@flow
            }

            val items = withContext(appDispatchers.io) {
                api.getCatalogItems(
                    page = page,
                    catalog = catalog,
                )
            }

            emit(Async.Success(items))
        } catch (e: Throwable) {
            errorLog(e)
            emit(
                Async.Failure(
                    UiText.from(
                        R.string.failed_to_fetch_catalog_items_format_message,
                        e.actualMessage,
                    ),
                )
            )
        }
    }
}
