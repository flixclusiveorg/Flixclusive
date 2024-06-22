package com.flixclusive.domain.category

import com.flixclusive.core.util.common.dispatcher.AppDispatchers
import com.flixclusive.core.util.common.dispatcher.Dispatcher
import com.flixclusive.core.util.common.resource.Resource
import com.flixclusive.core.util.common.ui.UiText
import com.flixclusive.core.util.exception.actualMessage
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.data.provider.ProviderApiRepository
import com.flixclusive.data.tmdb.TMDBRepository
import com.flixclusive.model.provider.ProviderCatalog
import com.flixclusive.model.tmdb.FilmSearchItem
import com.flixclusive.model.tmdb.SearchResponseData
import com.flixclusive.model.tmdb.category.Category
import com.flixclusive.provider.ProviderApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import com.flixclusive.core.util.R as UtilR

@Singleton
class CategoryItemsProviderUseCase @Inject constructor(
    private val tmdbRepository: TMDBRepository,
    private val providerApiRepository: ProviderApiRepository,
    @Dispatcher(AppDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(
        category: Category,
        page: Int
    ): Resource<SearchResponseData<FilmSearchItem>> {
        return when (category) {
            is ProviderCatalog -> {
                return try {
                    val api = category.providerApi!!
                    val items = withContext(ioDispatcher) {
                        api.getCatalogItems(
                            page = page,
                            catalog = category
                        )
                    }

                    Resource.Success(items)
                } catch (e: Exception) {
                    errorLog(e)
                    Resource.Failure(UiText.StringResource(UtilR.string.failed_to_fetch_catalog_items_format_message, e.actualMessage))
                }
            }
            else -> tmdbRepository.paginateConfigItems(
                url = category.url,
                page = page
            )
        }
    }

    private val ProviderCatalog.providerApi: ProviderApi?
        get() = providerApiRepository.apiMap[providerName]
}