package com.flixclusive.domain.search

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import com.flixclusive.core.util.common.dispatcher.di.ApplicationScope
import com.flixclusive.core.util.common.resource.Resource
import com.flixclusive.core.util.common.ui.UiText
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.data.configuration.AppConfigurationManager
import com.flixclusive.data.provider.ProviderManager
import com.flixclusive.data.tmdb.TMDBRepository
import com.flixclusive.model.tmdb.category.SearchCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.CancellationException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.random.Random
import com.flixclusive.core.util.R as UtilR

@Singleton
class GetSearchRecommendedCardsUseCase @Inject constructor(
    @ApplicationScope private val scope: CoroutineScope,
    private val tmdbRepository: TMDBRepository,
    private val configurationManager: AppConfigurationManager,
    private val providerManager: ProviderManager,
) {
    private val usedPosterPaths = mutableSetOf<String>()

    private val _tvShowNetworkCards = MutableStateFlow<List<SearchCategory>>(emptyList())
    val tvShowNetworkCards = _tvShowNetworkCards.asStateFlow()

    val providersCatalogsCards by derivedStateOf {
        providerManager.workingApis
            .flatMap {
                // In case some shitty code
                // might occur in the future here.
                safeCall { it.catalogs }
                    ?: emptyList()
            }
    }
    
    private val _movieCompanyCards = MutableStateFlow<List<SearchCategory>>(emptyList())
    val movieCompanyCards = _movieCompanyCards.asStateFlow()

    private val _cards = MutableStateFlow<Resource<List<SearchCategory>>>(Resource.Loading)
    val cards = _cards.asStateFlow()

    private var initializationJob: Job? = null

    init {
        // Start initializing so no need to run it again.
        invoke()
    }

    operator fun invoke() {
        val isAlreadyInitialized = configurationManager.searchCategoriesData?.run {
            (type.size + genres.size) == _cards.value.data?.size
        } ?: false

        if(initializationJob?.isActive == true || isAlreadyInitialized)
            return

        initializationJob = scope.launch {
            var newList = emptyList<SearchCategory>()

            configurationManager.searchCategoriesData?.run {
                val defaultErrorMessage = Resource.Failure(UtilR.string.failed_to_initialize_search_items)

                _cards.value = Resource.Loading
                _tvShowNetworkCards.value = configurationManager.searchCategoriesData?.networks?.shuffled()
                    ?: return@launch _cards.emit(defaultErrorMessage)
                _movieCompanyCards.value = configurationManager.searchCategoriesData?.companies?.shuffled()
                    ?: return@launch _cards.emit(defaultErrorMessage)

                (type + genres).map { item ->
                    // If item is reality shows,
                    // then use only its page 1.
                    val randomPage = max(1, Random.nextInt(1, 3000) % 5)
                    val pageToUse = if (item.name.equals("reality", true)) 1 else randomPage

                    when (
                        val result = tmdbRepository.paginateConfigItems(
                            url = item.url, page = pageToUse
                        )
                    ) {
                        is Resource.Failure -> {
                            _cards.emit(result)
                            currentCoroutineContext().cancel(CancellationException())
                            return@map
                        }
                        is Resource.Success -> {
                            result.data?.run {
                                var imageToUse: String? = null

                                if (results.isEmpty())
                                    return@map

                                while (
                                    usedPosterPaths.contains(imageToUse)
                                    || imageToUse == null
                                ) {
                                    imageToUse = results.random().backdropImage
                                }

                                newList = newList + item.copy(image = imageToUse)
                                usedPosterPaths.add(imageToUse)

                                _cards.emit(Resource.Success(newList))
                            }
                        }
                        else -> Unit
                    }
                }
            } ?: _cards.emit(Resource.Failure(error = UiText.StringResource(UtilR.string.failed_to_init_app)))
        }
    }
}