package com.flixclusive.feature.mobile.search

import androidx.compose.ui.util.fastMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.domain.catalog.model.DiscoverCards
import com.flixclusive.domain.catalog.usecase.GetDiscoverCardsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class SearchScreenViewModel @Inject constructor(
    private val getDiscoverCards: GetDiscoverCardsUseCase,
    private val providerRepository: ProviderRepository,
    private val dataStoreManager: DataStoreManager,
    private val userSessionDataStore: UserSessionDataStore,
) : ViewModel() {
    private val _cards = MutableStateFlow<Resource<DiscoverCards>>(Resource.Loading)
    val cards = _cards.asStateFlow()

    val providersCatalogsCards = userSessionDataStore.currentUserId
        .filterNotNull()
        .flatMapLatest { userId ->
            providerRepository
                .getEnabledProvidersAsFlow(ownerId = userId)
                .mapLatest { list ->
                    list.mapNotNull { provider ->
                        val api = safeCall {
                            providerRepository.getApi(
                                id = provider.id,
                                ownerId = userId,
                            )
                        }

                        api?.catalogs?.fastMap {
                            it.copy(providerId = provider.id)
                        }
                    }.flatten()
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = emptyList(),
        )

    init {
        initializeCards()
    }

    fun initializeCards() {
        viewModelScope.launch {
            _cards.value = getDiscoverCards()
        }
    }
}
