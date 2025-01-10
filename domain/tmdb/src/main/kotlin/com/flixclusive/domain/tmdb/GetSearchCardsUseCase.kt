package com.flixclusive.domain.tmdb

import com.flixclusive.core.locale.UiText
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.util.coroutines.AppDispatchers
import com.flixclusive.data.configuration.AppConfigurationManager
import com.flixclusive.data.tmdb.TMDBRepository
import com.flixclusive.model.configuration.catalog.SearchCatalog
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
import com.flixclusive.core.locale.R as LocaleR

@Singleton
class GetSearchCardsUseCase
    @Inject
    constructor(
        private val tmdbRepository: TMDBRepository,
        private val configurationManager: AppConfigurationManager,
    ) {
        private val usedPosterPaths = mutableSetOf<String>()

        private val _tvShowNetworkCards = MutableStateFlow<List<SearchCatalog>>(emptyList())
        val tvShowNetworkCards = _tvShowNetworkCards.asStateFlow()

        private val _movieCompanyCards = MutableStateFlow<List<SearchCatalog>>(emptyList())
        val movieCompanyCards = _movieCompanyCards.asStateFlow()

        private val _cards = MutableStateFlow<Resource<List<SearchCatalog>>>(Resource.Loading)
        val cards = _cards.asStateFlow()

        private var initializationJob: Job? = null

        init {
            // Start initializing so no need to run it again.
            invoke()
        }

        operator fun invoke() {
            val isAlreadyInitialized =
                configurationManager.searchCatalogsData?.run {
                    (type.size + genres.size) == _cards.value.data?.size
                } == true

            if (initializationJob?.isActive == true || isAlreadyInitialized) {
                return
            }

            initializationJob =
                AppDispatchers.Default.scope.launch {
                    var newList = emptyList<SearchCatalog>()

                    configurationManager.searchCatalogsData?.run {
                        val defaultErrorMessage = Resource.Failure(LocaleR.string.failed_to_initialize_search_items)

                        _cards.value = Resource.Loading
                        _tvShowNetworkCards.value = configurationManager.searchCatalogsData?.networks?.shuffled()
                            ?: return@launch _cards.emit(defaultErrorMessage)
                        _movieCompanyCards.value = configurationManager.searchCatalogsData?.companies?.shuffled()
                            ?: return@launch _cards.emit(defaultErrorMessage)

                        (type + genres).map { item ->
                            // If item is reality shows,
                            // then use only its page 1.
                            val randomPage = max(1, Random.nextInt(1, 3000) % 5)
                            val pageToUse = if (item.name.equals("reality", true)) 1 else randomPage

                            when (
                                val result =
                                    tmdbRepository.paginateConfigItems(
                                        url = item.url,
                                        page = pageToUse,
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

                                        if (results.isEmpty()) {
                                            return@map
                                        }

                                        while (
                                            usedPosterPaths.contains(imageToUse) ||
                                            imageToUse == null
                                        ) {
                                            imageToUse = results.random().backdropImage
                                        }

                                        newList = newList + item.copy(image = imageToUse)
                                        usedPosterPaths.add(imageToUse)

                                        _cards.emit(Resource.Success(newList))
                                    }
                                }

                                else -> {
                                    Unit
                                }
                            }
                        }
                    } ?: _cards.emit(Resource.Failure(error = UiText.StringResource(LocaleR.string.failed_to_init_app)))
                }
        }
    }
