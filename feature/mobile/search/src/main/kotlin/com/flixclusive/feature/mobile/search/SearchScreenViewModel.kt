package com.flixclusive.feature.mobile.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.network.util.Resource
import com.flixclusive.data.util.InternetMonitor
import com.flixclusive.domain.search.GetSearchRecommendedCardsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class SearchScreenViewModel @Inject constructor(
    private val getSearchRecommendedCardsUseCase: GetSearchRecommendedCardsUseCase,
    internetMonitor: InternetMonitor,
) : ViewModel() {
    val genreCards = getSearchRecommendedCardsUseCase.cards
    val tvShowNetworkCards = getSearchRecommendedCardsUseCase.tvShowNetworkCards
    val providersCatalogsCards = getSearchRecommendedCardsUseCase.providersCatalogsCards
    val movieCompanyCards = getSearchRecommendedCardsUseCase.movieCompanyCards

    init {
        viewModelScope.launch {
            internetMonitor.isOnline
                .combine(getSearchRecommendedCardsUseCase.cards) { isConnected, status ->
                    isConnected to status
                }
                .collectLatest { (isConnected, status) ->
                if (isConnected && status is Resource.Failure || status is Resource.Loading) {
                    getSearchRecommendedCardsUseCase()
                }
            }
        }
    }

    fun retryLoadingCards() {
        getSearchRecommendedCardsUseCase()
    }
}