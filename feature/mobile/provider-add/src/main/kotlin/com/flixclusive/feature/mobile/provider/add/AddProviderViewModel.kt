package com.flixclusive.feature.mobile.provider.add

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.locale.UiText
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.ui.mobile.component.provider.ProviderInstallationStatus
import com.flixclusive.core.util.coroutines.AppDispatchers
import com.flixclusive.core.util.coroutines.asStateFlow
import com.flixclusive.data.provider.ProviderRepository
import com.flixclusive.domain.provider.GetOnlineProvidersUseCase
import com.flixclusive.domain.provider.ProviderLoaderUseCase
import com.flixclusive.domain.provider.ProviderUnloaderUseCase
import com.flixclusive.domain.provider.ProviderUpdaterUseCase
import com.flixclusive.domain.provider.util.DownloadFailed
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
        private val providerUpdaterUseCase: ProviderUpdaterUseCase,
        private val providerLoaderUseCase: ProviderLoaderUseCase,
        private val providerUnloaderUseCase: ProviderUnloaderUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(AddProviderUiState())
        val uiState = _uiState.asStateFlow()

        private var installSelectionJob: Job? = null

        private val providerJobs = HashMap<String, Job?>()
        val providerInstallationStatusMap = mutableStateMapOf<String, ProviderInstallationStatus>()

        private val _initializeError = MutableSharedFlow<Boolean>()
        val initializeError = _initializeError.asSharedFlow()

        private val _providerLoadErrors = MutableSharedFlow<List<String>>()
        val providerLoadErrors = _providerLoadErrors.asSharedFlow()

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
                            filters = it.filters,
                        )
                }.distinctUntilChanged()
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

                    val hasErrors = _uiState.value.failedToInitializeRepositories.isNotEmpty()

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
                    } else if (hasErrors) {
                        _initializeError.emit(true)
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
            if (installSelectionJob?.isActive == true) return

            installSelectionJob =
                AppDispatchers.Default.scope.launch {
                    val selectedProviders = _uiState.value.selectedProviders
                    _uiState.update { it.copy(selectedProviders = emptyList()) }

                    val errors = mutableListOf<String>()
                    selectedProviders.forEach { provider ->
                        val job = providerJobs[provider.id]
                        val installationStatus = providerInstallationStatusMap[provider.id]!!

                        if (job?.isActive == true ||
                            installationStatus.isInstalled ||
                            installationStatus.isInstalling
                        ) {
                            return@forEach
                        }

                        providerJobs[provider.id] = null
                        providerInstallationStatusMap[provider.id] = ProviderInstallationStatus.Installing

                        try {
                            if (installationStatus.isOutdated) {
                                providerUpdaterUseCase.update(provider.id)
                            } else {
                                providerLoaderUseCase.load(
                                    provider = provider,
                                    needsDownload = true,
                                )
                            }
                        } catch (_: Exception) {
                            errors.add(provider.name)
                        }

                        val isInstalled = providerRepository.getProviderMetadata(provider.id) != null
                        val status =
                            when (isInstalled) {
                                true -> ProviderInstallationStatus.Installed
                                false -> ProviderInstallationStatus.NotInstalled
                            }

                        providerInstallationStatusMap[provider.id] = status
                    }

                    _providerLoadErrors.emit(errors)
                }
        }

        fun onToggleInstallation(provider: ProviderMetadata) {
            val job = providerJobs[provider.id]
            if (job?.isActive == true || installSelectionJob?.isActive == true) return

            providerJobs[provider.id] =
                AppDispatchers.Default.scope.launch {
                    when (providerInstallationStatusMap[provider.id]) {
                        ProviderInstallationStatus.NotInstalled -> installProvider(provider)
                        ProviderInstallationStatus.Installed -> uninstallProvider(provider)
                        ProviderInstallationStatus.Outdated -> updateProvider(provider)
                        else -> Unit
                    }
                }
        }

        private suspend fun updateProvider(provider: ProviderMetadata) {
            try {
                providerUpdaterUseCase.update(provider.id)
                providerInstallationStatusMap[provider.id] = ProviderInstallationStatus.Installed
            } catch (_: DownloadFailed) {
                _providerLoadErrors.emit(listOf(provider.name))
            }
        }

        private suspend fun installProvider(provider: ProviderMetadata): Boolean {
            providerInstallationStatusMap[provider.id] = ProviderInstallationStatus.Installing

            try {
                providerLoaderUseCase.load(
                    provider = provider,
                    needsDownload = true,
                )
            } catch (_: Exception) {
                _providerLoadErrors.emit(listOf(provider.name))
            }

            val isInstalled = providerRepository.getProviderMetadata(provider.id) != null
            val status =
                when (isInstalled) {
                    true -> ProviderInstallationStatus.Installed
                    false -> ProviderInstallationStatus.NotInstalled
                }

            providerInstallationStatusMap[provider.id] = status
            return isInstalled
        }

        private suspend fun uninstallProvider(metadata: ProviderMetadata) {
            providerUnloaderUseCase.unload(metadata)
            providerInstallationStatusMap[metadata.id] = ProviderInstallationStatus.NotInstalled
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

            loadProvidersInstallationState(providers)
            _uiState.update {
                it.copy(
                    failedToInitializeRepositories = failedToLoad.toList(),
                    availableProviders = providers.map { SearchableProvider.from(it) },
                )
            }
        }

        private fun loadProvidersInstallationState(providers: List<ProviderMetadata>) {
            providers.forEach { provider ->
                var status = ProviderInstallationStatus.NotInstalled

                val metadata = providerRepository.getProviderMetadata(provider.id)
                val isInstalledAlready = metadata != null

                if (isInstalledAlready && isOutdated(old = metadata!!, new = provider)) {
                    status = ProviderInstallationStatus.Outdated
                } else if (isInstalledAlready) {
                    status = ProviderInstallationStatus.Installed
                }

                providerInstallationStatusMap[provider.id] = status
            }
        }

        private fun isOutdated(
            old: ProviderMetadata,
            new: ProviderMetadata,
        ): Boolean {
            val provider = providerRepository.getProvider(old.id) ?: return false

            val manifest = provider.manifest
            if (manifest.updateUrl == null || manifest.updateUrl.equals("")) {
                return false
            }

            return manifest.versionCode < new.versionCode
        }

        private fun List<SearchableProvider>.filter(
            searchQuery: String,
            filters: List<AddProviderFilterType<*>>,
        ): List<ProviderMetadata> {
            var newSearchableList = this

            if (searchQuery.isNotEmpty()) {
                newSearchableList =
                    newSearchableList.fastFilter { provider ->
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
    val errorMessage: UiText? = null,
) {
    val hasInitializationErrors: Boolean get() = failedToInitializeRepositories.isNotEmpty()
}

@Immutable
internal data class SearchableProvider(
    val provider: ProviderMetadata,
    val searchText: String,
) {
    companion object {
        fun from(provider: ProviderMetadata): SearchableProvider {
            val searchText =
                buildString {
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
