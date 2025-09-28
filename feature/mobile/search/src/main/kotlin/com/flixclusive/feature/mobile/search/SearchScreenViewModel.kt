package com.flixclusive.feature.mobile.search

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.network.util.Resource
import com.flixclusive.data.provider.repository.ProviderApiRepository
import com.flixclusive.domain.catalog.model.DiscoverCards
import com.flixclusive.domain.catalog.usecase.GetDiscoverCardsUseCase
import com.flixclusive.model.provider.ProviderCatalog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class SearchScreenViewModel
    @Inject
    constructor(
        private val getDiscoverCards: GetDiscoverCardsUseCase,
        private val providerApiRepository: ProviderApiRepository,
    ) : ViewModel() {
        private val _cards = MutableStateFlow<Resource<DiscoverCards>>(Resource.Loading)
        val cards = _cards.asStateFlow()

        val providersCatalogsCards = mutableStateListOf<ProviderCatalog>()

        private val catalogsCardsOperationsHandler = ProviderCatalogsChangesHandler(providersCatalogsCards)

        init {
            initializeCards()

            viewModelScope.launch {
                providersCatalogsCards.addAll(
                    providerApiRepository
                        .getApis()
                        .flatMap { it.catalogs },
                )

                providerApiRepository
                    .observe()
                    .collect(catalogsCardsOperationsHandler::handleOperations)
            }
        }

        fun initializeCards() {
            viewModelScope.launch {
                _cards.value = getDiscoverCards()
        }
    }
}
