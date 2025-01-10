package com.flixclusive.feature.mobile.search

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.network.util.Resource
import com.flixclusive.data.provider.ProviderApiRepository
import com.flixclusive.data.util.InternetMonitor
import com.flixclusive.domain.tmdb.GetSearchCardsUseCase
import com.flixclusive.model.provider.ProviderCatalog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class SearchScreenViewModel
    @Inject
    constructor(
        private val getSearchRecommendedCardsUseCase: GetSearchCardsUseCase,
        private val providerApiRepository: ProviderApiRepository,
        internetMonitor: InternetMonitor,
    ) : ViewModel() {
        val genreCards = getSearchRecommendedCardsUseCase.cards
        val tvShowNetworkCards = getSearchRecommendedCardsUseCase.tvShowNetworkCards
        val movieCompanyCards = getSearchRecommendedCardsUseCase.movieCompanyCards
        val providersCatalogsCards = mutableStateListOf<ProviderCatalog>()

        private val catalogsCardsOperationsHandler = ProviderCatalogsChangesHandler(providersCatalogsCards)

        init {
            viewModelScope.launch {
                launch {
                    internetMonitor.isOnline
                        .combine(getSearchRecommendedCardsUseCase.cards) { isConnected, status ->
                            isConnected to status
                        }.collectLatest { (isConnected, status) ->
                            if (isConnected && status is Resource.Failure || status is Resource.Loading) {
                                getSearchRecommendedCardsUseCase()
                            }
                        }
                }

                launch {
                    providersCatalogsCards.addAll(
                        providerApiRepository
                            .getApis()
                            .flatMap { it.catalogs },
                    )

                    providerApiRepository.observe().collect {
                        catalogsCardsOperationsHandler.handleOperations(it)
                    }
                }
            }
        }

        fun retryLoadingCards() {
            getSearchRecommendedCardsUseCase()
        }
    }
