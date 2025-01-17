package com.flixclusive.feature.mobile.provider.add

import androidx.compose.runtime.Immutable
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.util.coroutines.asStateFlow
import com.flixclusive.data.provider.ProviderRepository
import com.flixclusive.domain.provider.GetOnlineProvidersUseCase
import com.flixclusive.domain.provider.util.extractGithubInfoFromLink
import com.flixclusive.feature.mobile.provider.add.filter.AddProviderFilterType
import com.flixclusive.feature.mobile.provider.add.filter.AuthorsFilters
import com.flixclusive.feature.mobile.provider.add.filter.CommonSortFilters
import com.flixclusive.feature.mobile.provider.add.filter.LanguagesFilters
import com.flixclusive.feature.mobile.provider.add.filter.ProviderTypeFilters
import com.flixclusive.feature.mobile.provider.add.filter.RepositoriesFilters
import com.flixclusive.feature.mobile.provider.add.filter.StatusFilters
import com.flixclusive.feature.mobile.provider.add.util.REPOSITORY_NAME_OWNER_FORMAT
import com.flixclusive.feature.mobile.provider.add.util.filterAuthors
import com.flixclusive.feature.mobile.provider.add.util.filterLanguages
import com.flixclusive.feature.mobile.provider.add.util.filterProviderType
import com.flixclusive.feature.mobile.provider.add.util.filterRepositories
import com.flixclusive.feature.mobile.provider.add.util.filterStatus
import com.flixclusive.feature.mobile.provider.add.util.loadAuthorFilters
import com.flixclusive.feature.mobile.provider.add.util.loadLanguageFilters
import com.flixclusive.feature.mobile.provider.add.util.loadProviderTypeFilters
import com.flixclusive.feature.mobile.provider.add.util.loadRepositoryFilters
import com.flixclusive.feature.mobile.provider.add.util.loadSortFilters
import com.flixclusive.feature.mobile.provider.add.util.loadStatusFilters
import com.flixclusive.feature.mobile.provider.add.util.sort
import com.flixclusive.model.datastore.user.ProviderPreferences
import com.flixclusive.model.datastore.user.UserPreferences
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.model.provider.Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
internal class AddProviderViewModel
    @Inject
    constructor(
        dataStoreManager: DataStoreManager,
        private val providerRepository: ProviderRepository,
        private val getOnlineProvidersUseCase: GetOnlineProvidersUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(AddProviderUiState())
        val uiState = _uiState.asStateFlow()

        private val _error = MutableSharedFlow<Boolean>()
        val error = _error.asSharedFlow()

        private val repositoriesAsFlow =
            dataStoreManager
                .getUserPrefs<ProviderPreferences>(UserPreferences.PROVIDER_PREFS_KEY)
                .mapLatest { it.repositories }
                .asStateFlow(viewModelScope)

        val selectedProviders =
            uiState
                .mapLatest { it.selectedProviders }
                .distinctUntilChanged()
                .asStateFlow(viewModelScope)

        val filters =
            uiState
                .mapLatest { it.filters }
                .distinctUntilChanged()
                .asStateFlow(viewModelScope)

        val availableProviders =
            uiState
                .mapLatest {
                    it.availableProviders
                        .filter(
                            searchQuery = it.searchQuery,
                            filters = it.filters
                        )
                }
                .distinctUntilChanged()
                .asStateFlow(viewModelScope)

        private var initJob: Job? = null

        init {
            observeRepositoriesToReinitialize()
        }

        fun initialize() {
            if (initJob?.isActive == true) return

            initJob =
                viewModelScope.launch {
                    _uiState.update {
                        it.copy(
                            isLoading = true,
                            filters = emptyList(),
                            availableProviders = emptyList(),
                            selectedProviders = emptyList(),
                            failedToInitializeRepositories = emptyList(),
                        )
                    }

                    val repositories = repositoriesAsFlow.first()
                    loadAvailableProviders(repositories)
                    val providers = availableProviders.first()

                    if (providers.isNotEmpty()) {
                        val sortFilters = loadSortFilters()
                        val repositoryFilters = providers.loadRepositoryFilters(repositories)
                        val authorFilters = providers.loadAuthorFilters()
                        val languageFilters = providers.loadLanguageFilters()
                        val providerTypeFilters = providers.loadProviderTypeFilters()
                        val statusFilters = providers.loadStatusFilters()

                        _uiState.update {
                            it.copy(
                                filters =
                                    listOf(
                                        sortFilters,
                                        repositoryFilters,
                                        authorFilters,
                                        languageFilters,
                                        providerTypeFilters,
                                        statusFilters,
                                    ),
                            )
                        }
                    } else if (_uiState.value.hasErrors) {
                        _error.emit(true)
                    }

                    _uiState.update {
                        it.copy(isLoading = false)
                    }
                }
        }

        private fun observeRepositoriesToReinitialize() {
            viewModelScope.launch {
                repositoriesAsFlow.collectLatest {
                    initialize()
                }
            }
        }

        fun onSearchQueryChange(query: String) {
            _uiState.update {
                it.copy(searchQuery = query)
            }
        }

        fun onToggleFilterSheet(state: Boolean) {
            _uiState.update {
                it.copy(isShowingFilterSheet = state)
            }
        }

        fun onInstallSelection() {
            // TODO("Not implemented yet")
        }

        fun onUpdateFilter(
            index: Int,
            filter: AddProviderFilterType<*>,
        ) {
            _uiState.update {
                val newList = it.filters.toMutableList()
                newList[index] = filter
                it.copy(filters = newList.toList())
            }
        }

        fun onUnselectAll() {
            _uiState.update {
                it.copy(selectedProviders = emptyList())
            }
        }

        fun onToggleSelect(provider: ProviderMetadata) {
            _uiState.update {
                val newList =
                    if (it.selectedProviders.contains(provider)) {
                        val newList = it.selectedProviders.toMutableList()
                        newList.remove(provider)
                        newList.toList()
                    } else {
                        it.selectedProviders + provider
                    }

                it.copy(selectedProviders = newList)
            }
        }

        fun onToggleSearchBar(state: Boolean) {
            _uiState.update {
                it.copy(isShowingSearchBar = state)
            }
        }

        private suspend fun loadAvailableProviders(repositories: List<Repository>) {
            val failedToLoad = mutableListOf<Repository>()
            val providers = mutableListOf<ProviderMetadata>()
            repositories.fastForEach { repository ->
                when (val result = getOnlineProvidersUseCase(repository)) {
                    Resource.Loading -> Unit
                    is Resource.Failure -> failedToLoad.add(repository)
                    is Resource.Success -> providers.addAll(result.data!!)
                }
            }

            _uiState.update {
                it.copy(
                    failedToInitializeRepositories = failedToLoad.toList(),
                    availableProviders = providers.map { SearchableProvider.from(it) },
                )
            }
        }

        private fun List<SearchableProvider>.filter(
            searchQuery: String,
            filters: List<AddProviderFilterType<*>>
        ): List<ProviderMetadata> {
            var newSearchableList = this

            if (searchQuery.isNotEmpty()) {
                newSearchableList = newSearchableList.fastFilter { provider ->
                    provider.searchText.contains(searchQuery, true)
                }
            }

            var newList = newSearchableList.fastMap { it.provider }
            filters.fastForEach { filter ->
                newList =
                    when (filter) {
                        is CommonSortFilters -> newList.sort(filter)
                        is AuthorsFilters -> newList.filterAuthors(filter)
                        is RepositoriesFilters -> newList.filterRepositories(filter)
                        is LanguagesFilters -> newList.filterLanguages(filter)
                        is ProviderTypeFilters -> newList.filterProviderType(filter)
                        is StatusFilters -> newList.filterStatus(filter)
                        else -> throw IllegalArgumentException("Invalid filter provided: $filter")
                    }
            }

            return newList
        }
    }

@Immutable
internal data class AddProviderUiState(
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val isShowingFilterSheet: Boolean = false,
    val isShowingSearchBar: Boolean = false,
    val filters: List<AddProviderFilterType<*>> = emptyList(),
    val availableProviders: List<SearchableProvider> = emptyList(),
    val selectedProviders: List<ProviderMetadata> = emptyList(),
    val failedToInitializeRepositories: List<Repository> = emptyList(),
) {
    val hasErrors: Boolean get() = failedToInitializeRepositories.isNotEmpty()
}

internal class SearchableProvider(
    val provider: ProviderMetadata,
    val searchText: String
) {
    companion object {
        fun from(provider: ProviderMetadata): SearchableProvider {
            val searchText = buildString {
                append(provider.id)
                append(provider.name)
                provider.description?.let { append(it) }
                append(provider.providerType.type)
                append(provider.language.languageCode)
                provider.authors.forEach { append(it) }

                extractGithubInfoFromLink(provider.repositoryUrl)?.let { (username, repository) ->
                    append(String.format(Locale.getDefault(), REPOSITORY_NAME_OWNER_FORMAT, username, repository))
                }

                append(provider.versionName)
            }

            return SearchableProvider(provider, searchText)
        }
    }
}
