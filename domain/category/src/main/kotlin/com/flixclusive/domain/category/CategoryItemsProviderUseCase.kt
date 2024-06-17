package com.flixclusive.domain.category

import com.flixclusive.core.util.common.resource.Resource
import com.flixclusive.core.util.common.ui.UiText
import com.flixclusive.data.provider.ProviderApiRepository
import com.flixclusive.data.tmdb.TMDBRepository
import com.flixclusive.model.provider.ProviderCatalog
import com.flixclusive.model.tmdb.FilmSearchItem
import com.flixclusive.model.tmdb.SearchResponseData
import com.flixclusive.model.tmdb.category.Category
import com.flixclusive.provider.ProviderApi
import javax.inject.Inject
import javax.inject.Singleton
import com.flixclusive.core.util.R as UtilR

@Singleton
class CategoryItemsProviderUseCase @Inject constructor(
    private val tmdbRepository: TMDBRepository,
    private val providerApiRepository: ProviderApiRepository,
) {
    suspend operator fun invoke(
        category: Category,
        page: Int
    ): Resource<SearchResponseData<FilmSearchItem>> {
        return when (category) {
            is ProviderCatalog -> {
                return try {
                    val api = category.providerApi!!
                    val items = api.getCatalogItems(
                        page = page,
                        catalog = category
                    )

                    Resource.Success(items)
                } catch (e: Exception) {
                    Resource.Failure(UiText.StringResource(UtilR.string.failed_to_fetch_catalog_items_format_message, e.localizedMessage ?: "UNKNOWN ERR"))
                }
            }
            else -> tmdbRepository.paginateConfigItems(
                url = category.url,
                page = page
            )
        }
    }

    private val ProviderCatalog.providerApi: ProviderApi?
        get() = providerApiRepository.apiMap[name]
}