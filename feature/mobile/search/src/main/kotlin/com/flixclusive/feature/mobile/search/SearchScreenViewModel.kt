package com.flixclusive.feature.mobile.search

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.data.provider.repository.ProviderApiRepository
import com.flixclusive.data.tmdb.repository.TMDBDiscoverCatalogRepository
import com.flixclusive.model.provider.ProviderCatalog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class SearchScreenViewModel
    @Inject
    constructor(
        private val tmdbDiscoverCatalogRepository: TMDBDiscoverCatalogRepository,
        private val providerApiRepository: ProviderApiRepository,
    ) : ViewModel() {
        val genreCards = flow { emit(tmdbDiscoverCatalogRepository.getGenres()) }
            .stateIn(
                viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = emptyList(),
            )

        val tvShowNetworkCards = flow { emit(tmdbDiscoverCatalogRepository.getTvNetworks()) }
            .stateIn(
                viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = emptyList(),
            )

        val movieCompanyCards = flow { emit(tmdbDiscoverCatalogRepository.getMovieCompanies()) }
            .stateIn(
                viewModelScope,
                started = SharingStarted.Eagerly,
            initialValue = emptyList(),
        )

        val providersCatalogsCards = mutableStateListOf<ProviderCatalog>()

        private val catalogsCardsOperationsHandler = ProviderCatalogsChangesHandler(providersCatalogsCards)

        init {
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
}
