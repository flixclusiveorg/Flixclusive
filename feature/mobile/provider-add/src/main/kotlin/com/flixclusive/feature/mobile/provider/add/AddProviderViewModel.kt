package com.flixclusive.feature.mobile.provider.add

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastFold
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.common.provider.ProviderInstallationStatus
import com.flixclusive.core.common.provider.ProviderWithThrowable
import com.flixclusive.core.database.entity.provider.InstalledProvider
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.util.log.infoLog
import com.flixclusive.core.util.log.warnLog
import com.flixclusive.data.provider.repository.InstalledRepoRepository
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.domain.provider.usecase.get.GetProviderFromRemoteUseCase
import com.flixclusive.domain.provider.usecase.manage.InstallProviderUseCase
import com.flixclusive.domain.provider.usecase.manage.LoadProviderUseCase
import com.flixclusive.domain.provider.usecase.manage.ProviderResult
import com.flixclusive.domain.provider.usecase.manage.UnloadProviderUseCase
import com.flixclusive.domain.provider.usecase.updater.UpdateProviderUseCase
import com.flixclusive.domain.provider.util.extensions.toRepository
import com.flixclusive.domain.provider.util.extractGithubInfoFromLink
import com.flixclusive.feature.mobile.provider.add.filter.AddProviderFilterType
import com.flixclusive.feature.mobile.provider.add.filter.AuthorsFilters
import com.flixclusive.feature.mobile.provider.add.filter.AuthorsFilters.Companion.filterAuthors
import com.flixclusive.feature.mobile.provider.add.filter.AuthorsFilters.Companion.toAuthorFilters
import com.flixclusive.feature.mobile.provider.add.filter.CommonSortFilters
import com.flixclusive.feature.mobile.provider.add.filter.CommonSortFilters.Companion.sort
import com.flixclusive.feature.mobile.provider.add.filter.LanguagesFilters
import com.flixclusive.feature.mobile.provider.add.filter.LanguagesFilters.Companion.filterLanguages
import com.flixclusive.feature.mobile.provider.add.filter.LanguagesFilters.Companion.toLanguageFilters
import com.flixclusive.feature.mobile.provider.add.filter.ProviderTypeFilters
import com.flixclusive.feature.mobile.provider.add.filter.ProviderTypeFilters.Companion.filterProviderType
import com.flixclusive.feature.mobile.provider.add.filter.ProviderTypeFilters.Companion.toProviderTypeFilters
import com.flixclusive.feature.mobile.provider.add.filter.RepositoriesFilters
import com.flixclusive.feature.mobile.provider.add.filter.RepositoriesFilters.Companion.REPOSITORY_NAME_OWNER_FORMAT
import com.flixclusive.feature.mobile.provider.add.filter.RepositoriesFilters.Companion.filterRepositories
import com.flixclusive.feature.mobile.provider.add.filter.RepositoriesFilters.Companion.toRepositoryFilters
import com.flixclusive.feature.mobile.provider.add.filter.StatusFilters
import com.flixclusive.feature.mobile.provider.add.filter.StatusFilters.Companion.filterStatus
import com.flixclusive.feature.mobile.provider.add.filter.StatusFilters.Companion.toStatusFilters
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.model.provider.Repository
import com.ramcosta.composedestinations.generated.provideradd.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
internal class AddProviderViewModel @Inject constructor(
    private val userSessionDataStore: UserSessionDataStore,
    private val installedRepoRepository: InstalledRepoRepository,
    private val providerRepository: ProviderRepository,
    private val getProviderFromRemote: GetProviderFromRemoteUseCase,
    private val _updateProvider: UpdateProviderUseCase,
    private val installProvider: InstallProviderUseCase,
    private val loadProvider: LoadProviderUseCase,
    private val unloadProvider: UnloadProviderUseCase,
    private val appDispatchers: AppDispatchers,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val navArgs = savedStateHandle.navArgs<AddProviderScreenNavArgs>()

    private val _uiState = MutableStateFlow(AddProviderUiState())
    val uiState = _uiState.asStateFlow()

    private var installSelectionJob: Job? = null
    private var initJob: Job? = null

    private val providerJobs = HashMap<String, Job?>()
    val providerInstallationStatusMap = mutableStateMapOf<String, ProviderInstallationStatus>()

    private val _selected = MutableStateFlow(persistentSetOf<ProviderMetadata>())
    val selected = _selected.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _filters = MutableStateFlow(persistentListOf<AddProviderFilterType<*>>())
    val filters = _filters.asStateFlow()

    private val _availableProviders = MutableStateFlow(persistentListOf<SearchableProvider>())
    val availableProviders = _availableProviders
        .asStateFlow()
        .combine(filters) { providers, currentFilters ->
            // Apply filters first then search query
            // This way, the every time the query changes,
            // we don't have to re-apply the filters
            currentFilters
                .fastFold(providers as List<SearchableProvider>) { currentList, filter ->
                    when (filter) {
                        is CommonSortFilters -> currentList.sort(filter)
                        is AuthorsFilters -> currentList.filterAuthors(filter)
                        is RepositoriesFilters -> currentList.filterRepositories(filter)
                        is LanguagesFilters -> currentList.filterLanguages(filter)
                        is ProviderTypeFilters -> currentList.filterProviderType(filter)
                        is StatusFilters -> currentList.filterStatus(filter)
                        else -> throw IllegalArgumentException("Invalid filter provided: $filter")
                    }
                }.toPersistentList()
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = persistentListOf(),
        )

    val searchResults = availableProviders
        .combine(searchQuery) { providers, query ->
            if (query.isBlank()) {
                providers
            } else {
                providers
                    .fastFilter {
                        it.searchText.contains(query, ignoreCase = true)
                    }.toPersistentList()
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = persistentListOf(),
        )

    init {
        initialize()
    }

    fun initialize() {
        if (initJob?.isActive == true) return

        initJob = viewModelScope.launch {
            _selected.value = _selected.value.clear()
            _availableProviders.value = _availableProviders.value.clear()
            _uiState.update {
                it.copy(
                    isLoading = true,
                    repositoryExceptions = emptyList(),
                )
            }

            val userId = userSessionDataStore.currentUserId.filterNotNull().first()
            val repositories = installedRepoRepository
                .getAll(userId)
                .map { it.toRepository() }

            loadAvailableProviders(repositories) // Load providers from the given repositories

            val providers = _availableProviders.value

            val sortFilters = CommonSortFilters.create()
            val repositoryFilters = providers.toRepositoryFilters(
                repositories = repositories,
                initialSelectedRepository = navArgs.initialSelectedRepositoryFilter,
            )
            val authorFilters = providers.toAuthorFilters()
            val languageFilters = providers.toLanguageFilters()
            val providerTypeFilters = providers.toProviderTypeFilters()
            val statusFilters = providers.toStatusFilters()

            _filters.value = persistentListOf(
                sortFilters,
                repositoryFilters,
                authorFilters,
                languageFilters,
                providerTypeFilters,
                statusFilters,
            )

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onInstallSelection() {
        if (installSelectionJob?.isActive == true) return

        installSelectionJob = appDispatchers.ioScope.launch {
            _uiState.update { it.copy(isInstallingProviders = _selected.value.isNotEmpty()) }
            _selected.value.forEach { provider ->
                val job = providerJobs[provider.id]
                val installationStatus = providerInstallationStatusMap[provider.id]!!

                if (job?.isActive == true ||
                    installationStatus.isInstalled ||
                    installationStatus.isInstalling
                ) {
                    return@forEach
                }

                when (installationStatus) {
                    ProviderInstallationStatus.NotInstalled -> installAndLoadProvider(provider)
                    ProviderInstallationStatus.Installed -> uninstallProvider(provider)
                    ProviderInstallationStatus.Outdated -> updateProvider(provider)
                    else -> Unit
                }
            }
            _uiState.update { it.copy(isInstallingProviders = false) }
        }
    }

    /**
     * Toggles the installation status of the given provider.
     *
     * @param provider The provider to toggle installation for.
     * */
    fun onToggleInstallation(provider: ProviderMetadata) {
        val job = providerJobs[provider.id]
        if (job?.isActive == true || installSelectionJob?.isActive == true) return

        providerJobs[provider.id] = appDispatchers.ioScope.launch {
            when (providerInstallationStatusMap[provider.id]) {
                ProviderInstallationStatus.NotInstalled -> installAndLoadProvider(provider)
                ProviderInstallationStatus.Installed -> uninstallProvider(provider)
                ProviderInstallationStatus.Outdated -> updateProvider(provider)
                else -> Unit
            }
        }
    }

    /**
     * Updates the given provider.
     *
     * @param provider The provider to update.
     * */
    private suspend fun updateProvider(provider: ProviderMetadata) {
        try {
            infoLog("Updating and installing provider: ${provider.name}")
            providerInstallationStatusMap[provider.id] = ProviderInstallationStatus.Installing

            _updateProvider(provider)
            providerInstallationStatusMap[provider.id] = ProviderInstallationStatus.Installed
        } catch (e: Throwable) {
            _uiState.update {
                val error = ProviderWithThrowable(provider = provider, throwable = e)
                it.copy(providerExceptions = it.providerExceptions + error)
            }
        }
    }

    private suspend fun installAndLoadProvider(provider: ProviderMetadata) {
        try {
            providerInstallationStatusMap[provider.id] = ProviderInstallationStatus.Installing
            infoLog("Downloading and installing provider: ${provider.name}")

            installProvider(provider).collect {
                if (it is ProviderResult.Failure) throw it.error
            }

            val installedProvider = getInstalledProvider(provider.id)
                ?: error("Provider ${provider.name} not found after installation")

            loadProvider(installedProvider).collect {
                if (it is ProviderResult.Failure) throw it.error
            }

            providerInstallationStatusMap[provider.id] = ProviderInstallationStatus.Installed
        } catch (e: Throwable) {
            _uiState.update {
                val error = ProviderWithThrowable(provider = provider, throwable = e)
                it.copy(providerExceptions = it.providerExceptions + error)
            }

            val isInstalled = providerRepository.getMetadata(provider.id) != null
            val status = when {
                isInstalled -> ProviderInstallationStatus.Installed
                else -> ProviderInstallationStatus.NotInstalled
            }

            providerInstallationStatusMap[provider.id] = status
        }
    }

    private suspend fun uninstallProvider(provider: ProviderMetadata) {
        try {
            val installedProvider = getInstalledProvider(provider.id)
            if (installedProvider == null) {
                warnLog("Provider ${provider.name} was not found. Skipping uninstallation...")
                return
            }

            infoLog("Uninstalling provider: ${provider.name}")
            unloadProvider(installedProvider)
            providerInstallationStatusMap[provider.id] = ProviderInstallationStatus.NotInstalled
        } catch (e: Throwable) {
            _uiState.update {
                val error = ProviderWithThrowable(provider = provider, throwable = e)
                it.copy(providerExceptions = it.providerExceptions + error)
            }
        }
    }

    fun onUpdateFilter(
        index: Int,
        filter: AddProviderFilterType<*>,
    ) {
        _filters.update { it.set(index, filter) }
    }

    fun onUnselectAll() {
        _selected.update { it.clear() }
    }

    fun onToggleSelect(provider: ProviderMetadata) {
        _selected.update {
            if (it.contains(provider)) {
                it.remove(provider)
            } else {
                it.add(provider)
            }
        }
    }

    fun onToggleSearchBar(state: Boolean) {
        _uiState.update { it.copy(isShowingSearchBar = state) }
    }

    fun consumeProviderExceptions() {
        _uiState.update { it.copy(providerExceptions = emptyList()) }
    }

    /**
     * Loads providers from the given list of repositories.
     *
     * Errors from each repository are collected and stored in the UI state.
     * */
    private suspend fun loadAvailableProviders(repositories: List<Repository>) {
        repositories.forEach { repository ->
            when (val result = getProviderFromRemote(repository)) {
                Resource.Loading -> Unit
                is Resource.Failure -> {
                    val pair = repository to result.error!!
                    _uiState.update {
                        it.copy(repositoryExceptions = it.repositoryExceptions + pair)
                    }
                }

                is Resource.Success -> {
                    val providers = result.data!!

                    providers.fastForEach {
                        val provider = SearchableProvider.from(it)
                        var status = ProviderInstallationStatus.NotInstalled

                        val metadata = providerRepository.getMetadata(provider.id)
                        val isInstalled = metadata != null

                        if (isInstalled && isOutdated(old = metadata, new = provider.metadata)) {
                            status = ProviderInstallationStatus.Outdated
                        } else if (isInstalled) {
                            status = ProviderInstallationStatus.Installed
                        }

                        providerInstallationStatusMap[provider.id] = status

                        _availableProviders.value = _availableProviders.value.add(provider)
                    }
                }
            }
        }
    }

    private fun isOutdated(
        old: ProviderMetadata,
        new: ProviderMetadata,
    ): Boolean {
        val provider = providerRepository.getPlugin(old.id) ?: return false

        val manifest = provider.manifest
        if (manifest.updateUrl == null || manifest.updateUrl.equals("")) {
            return false
        }

        return manifest.versionCode < new.versionCode
    }

    private suspend fun getInstalledProvider(id: String): InstalledProvider? {
        val userId = userSessionDataStore.currentUserId.filterNotNull().first()
        return providerRepository.getInstalledProvider(
            id = id,
            ownerId = userId
        )
    }
}

@Immutable
internal data class AddProviderUiState(
    val isLoading: Boolean = false,
    val isInstallingProviders: Boolean = false,
    val repositoryExceptions: List<RepositoryWithError> = emptyList(),
    val providerExceptions: List<ProviderWithThrowable> = emptyList(),
    val isShowingSearchBar: Boolean = false,
)

internal typealias RepositoryWithError = Pair<Repository, UiText>

@Immutable
internal data class SearchableProvider(
    val metadata: ProviderMetadata,
    val searchText: String,
) {
    val id get() = metadata.id
    val name get() = metadata.name

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
