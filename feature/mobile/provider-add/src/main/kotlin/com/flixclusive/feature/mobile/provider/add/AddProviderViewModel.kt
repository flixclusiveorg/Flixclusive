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
import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.common.provider.ProviderWithThrowable
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.core.presentation.mobile.components.provider.ProviderInstallState
import com.flixclusive.core.util.log.infoLog
import com.flixclusive.core.util.log.warnLog
import com.flixclusive.data.provider.repository.InstalledRepoRepository
import com.flixclusive.domain.downloads.usecase.CancelDownloadUseCase
import com.flixclusive.domain.provider.usecase.get.GetInstalledProviderUseCase
import com.flixclusive.domain.provider.usecase.get.GetProviderFromRemoteUseCase
import com.flixclusive.domain.provider.usecase.get.GetProviderMetadataUseCase
import com.flixclusive.domain.provider.usecase.get.GetProviderPluginUseCase
import com.flixclusive.domain.provider.usecase.manage.DownloadProviderResult
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
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

// TODO: Make installAndLoadProvider a use case so it can work
//  seamlessly with the provider details screen when installing from there as well

@HiltViewModel
internal class AddProviderViewModel @Inject constructor(
    private val userSessionDataStore: UserSessionDataStore,
    private val installedRepoRepository: InstalledRepoRepository,
    private val getProviderFromRemote: GetProviderFromRemoteUseCase,
    private val getProviderMetadata: GetProviderMetadataUseCase,
    private val getProviderPlugin: GetProviderPluginUseCase,
    private val getInstalledProvider: GetInstalledProviderUseCase,
    private val updateProvider: UpdateProviderUseCase,
    private val installProvider: InstallProviderUseCase,
    private val loadProvider: LoadProviderUseCase,
    private val unloadProvider: UnloadProviderUseCase,
    private val appDispatchers: AppDispatchers,
    private val cancelDownload: CancelDownloadUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val navArgs = savedStateHandle.navArgs<AddProviderScreenNavArgs>()

    private val _uiState = MutableStateFlow(AddProviderUiState())
    val uiState = _uiState.asStateFlow()

    private var installSelectionJob: Job? = null
    private var initJob: Job? = null

    private val providerJobs = HashMap<String, Job?>()
    val providerInstallStates = mutableStateMapOf<String, ProviderInstallState>()

    private val _selected = MutableStateFlow(persistentSetOf<ProviderMetadata>())
    val selected = _selected.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _availableProviders = MutableStateFlow<Async<PersistentList<ProviderItem>>>(Async.Loading)
    private val _filters = MutableStateFlow<Async<PersistentList<AddProviderFilterType<*>>>>(Async.Loading)
    val filters = _filters.asStateFlow()

    val availableProviders = combine(_availableProviders, _filters) { asyncProviders, asyncFilters ->
        if (asyncProviders is Async.Loading || asyncFilters is Async.Loading) {
            return@combine Async.Loading
        }
        if (asyncProviders is Async.Failure) return@combine asyncProviders
        if (asyncFilters is Async.Failure) return@combine asyncFilters

        val providers = (asyncProviders as Async.Success).data
        val currentFilters = (asyncFilters as Async.Success).data
        Async.Success(
            currentFilters
                .fastFold(providers as List<ProviderItem>) { currentList, filter ->
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
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = Async.Loading,
    )

    val searchResults = availableProviders
        .combine(searchQuery) { asyncProviders, query ->
            if (asyncProviders !is Async.Success) return@combine asyncProviders
            if (query.isBlank()) return@combine asyncProviders
            Async.Success(
                asyncProviders.data
                    .fastFilter { it.searchText.contains(query, ignoreCase = true) }
                    .toPersistentList()
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = Async.Loading,
        )

    init {
        initialize()
    }

    private suspend fun onUpdateProvider(provider: ProviderMetadata) {
        val initialState = providerInstallStates[provider.id] ?: ProviderInstallState.NotInstalled
        try {
            updateProvider(provider).collect { result ->
                when (result) {
                    is DownloadProviderResult.Failure -> {
                        throw result.error
                    }

                    is DownloadProviderResult.Downloading -> {
                        providerInstallStates[provider.id] = ProviderInstallState.Installing(
                            progress = result.progress,
                            downloadId = result.downloadId,
                        )
                    }

                    is DownloadProviderResult.Success -> {
                        providerInstallStates[provider.id] = ProviderInstallState.Installed
                    }
                }
            }
        } catch (e: Throwable) {
            _uiState.update {
                it.copy(providerExceptions = it.providerExceptions + ProviderWithThrowable(provider, e))
            }
            providerInstallStates[provider.id] = initialState
        }
    }

    private suspend fun onInstallProvider(provider: ProviderMetadata) {
        val initialState = providerInstallStates[provider.id] ?: ProviderInstallState.NotInstalled
        try {
            infoLog("Downloading and installing provider: ${provider.name}")
            installProvider(provider)
                .onEach { result ->
                    when (result) {
                        is DownloadProviderResult.Failure -> {
                            throw result.error
                        }

                        is DownloadProviderResult.Downloading -> {
                            providerInstallStates[provider.id] = ProviderInstallState.Installing(
                                progress = result.progress,
                                downloadId = result.downloadId,
                            )
                        }

                        is DownloadProviderResult.Success -> {
                            Unit
                        }
                    }
                }.catch { throw it }
                .collect()

            val installedProvider = getInstalledProvider(provider.id)
                ?: error("Provider ${provider.name} not found after installation")

            loadProvider(installedProvider)
                .onEach { result ->
                    if (result is ProviderResult.Failure) throw result.error
                }.catch { throw it }
                .collect()

            providerInstallStates[provider.id] = ProviderInstallState.Installed
        } catch (e: Throwable) {
            _uiState.update {
                it.copy(providerExceptions = it.providerExceptions + ProviderWithThrowable(provider, e))
            }
            val isInstalled = getInstalledProvider(provider.id) != null
            providerInstallStates[provider.id] = if (isInstalled) ProviderInstallState.Installed else initialState
        }
    }

    private suspend fun onUninstallProvider(provider: ProviderMetadata) {
        try {
            val installedProvider = getInstalledProvider(provider.id)
            if (installedProvider == null) {
                warnLog("Provider ${provider.name} was not found. Skipping uninstallation...")
                return
            }

            infoLog("Uninstalling provider: ${provider.name}")
            providerInstallStates[provider.id] = ProviderInstallState.Uninstalling
            unloadProvider(installedProvider)
            providerInstallStates[provider.id] = ProviderInstallState.NotInstalled
        } catch (e: Throwable) {
            _uiState.update {
                it.copy(providerExceptions = it.providerExceptions + ProviderWithThrowable(provider, e))
            }
            providerInstallStates[provider.id] = ProviderInstallState.Installed
        }
    }

    private suspend fun onCancelDownload(
        downloadId: String,
        provider: ProviderMetadata
    ) {
        try {
            providerJobs[provider.id]?.cancel()
            cancelDownload(downloadId)
            providerInstallStates[provider.id] = getInstallState(provider)
        } catch (_: CancellationException) {
            // No-op
        } catch (e: Throwable) {
            _uiState.update {
                it.copy(providerExceptions = it.providerExceptions + ProviderWithThrowable(provider, e))
            }
        }
    }

    private suspend fun loadAvailableProviders(repositories: List<Repository>) {
        val providerList = mutableListOf<ProviderItem>()
        repositories.forEach { repository ->
            try {
                val providers = getProviderFromRemote(repository)
                providers.fastForEach { remote ->
                    providerInstallStates[remote.id] = getInstallState(remote)
                    providerList.add(ProviderItem.from(remote))
                }
            } catch (e: Throwable) {
                val pair = repository to UiText.from(e.message ?: "Unknown error")
                _uiState.update {
                    it.copy(repositoryExceptions = it.repositoryExceptions + pair)
                }
            }
        }

        if (providerList.isEmpty() && _uiState.value.repositoryExceptions.isNotEmpty()) {
            _availableProviders.value = Async.Failure(
                message = _uiState.value.repositoryExceptions
                    .first()
                    .second,
            )
        } else {
            _availableProviders.value = Async.Success(providerList.toPersistentList())
        }
    }

    private suspend fun getInstallState(remote: ProviderMetadata): ProviderInstallState {
        val localMetadata = getProviderMetadata(remote.id) ?: return ProviderInstallState.NotInstalled
        val plugin = getProviderPlugin(localMetadata.id) ?: return ProviderInstallState.Installed

        val manifest = plugin.manifest
        if (manifest.updateUrl == null || manifest.updateUrl.equals("")) {
            return ProviderInstallState.Installed
        }

        return if (manifest.versionCode < remote.versionCode) {
            ProviderInstallState.Outdated(
                newVersion = remote.versionName,
                newChangelogs = remote.changelog,
            )
        } else {
            ProviderInstallState.Installed
        }
    }

    fun initialize() {
        if (initJob?.isActive == true) return

        initJob = viewModelScope.launch {
            _selected.value = _selected.value.clear()
            providerInstallStates.clear()
            _availableProviders.value = Async.Loading
            _filters.value = Async.Loading
            _uiState.update { it.copy(repositoryExceptions = emptyList()) }

            val userId = userSessionDataStore.currentUserId.filterNotNull().first()
            val repositories = installedRepoRepository
                .getAll(userId)
                .map { it.toRepository() }

            loadAvailableProviders(repositories)

            val asyncProviders = _availableProviders.value
            if (asyncProviders is Async.Failure) {
                _filters.value = asyncProviders
                return@launch
            }

            val providers = (asyncProviders as Async.Success).data
            _filters.value = Async.Success(
                persistentListOf(
                    CommonSortFilters.create(),
                    providers.toRepositoryFilters(
                        repositories = repositories,
                        initialSelectedRepository = navArgs.initialSelectedRepositoryFilter,
                    ),
                    providers.toAuthorFilters(),
                    providers.toLanguageFilters(),
                    providers.toProviderTypeFilters(),
                    providers.toStatusFilters(),
                )
            )
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onInstallSelection() {
        if (installSelectionJob?.isActive == true) return

        installSelectionJob = appDispatchers.ioScope.launch {
            _uiState.update { it.copy(isInstallingProviders = _selected.value.isNotEmpty()) }

            val selected = _selected.value
            val batches = selected.chunked(3)

            _selected.value = _selected.value.clear()
            val oldTempInstallStates = providerInstallStates.toMap()

            selected.forEach {
                providerJobs[it.id]?.cancel()
                providerInstallStates[it.id] = ProviderInstallState.Installing(
                    progress = 0f,
                    downloadId = "batch-${it.id}",
                )
            }

            batches.forEach { batch ->
                val deferred = batch.map { selectedProvider ->
                    async {
                        val installState =
                            oldTempInstallStates[selectedProvider.id] ?: ProviderInstallState.NotInstalled
                        when (installState) {
                            is ProviderInstallState.NotInstalled -> onInstallProvider(selectedProvider)
                            is ProviderInstallState.Outdated -> onUpdateProvider(selectedProvider)
                            else -> Unit
                        }
                    }
                }

                deferred.awaitAll()
            }

            _uiState.update { it.copy(isInstallingProviders = false) }
        }
    }

    fun onToggleInstallState(provider: ProviderMetadata) {
        val job = providerJobs[provider.id]
        if (
            (job?.isActive == true || installSelectionJob?.isActive == true) &&
            providerInstallStates[provider.id] !is ProviderInstallState.Installing
        ) {
            return
        }

        providerJobs[provider.id] = appDispatchers.ioScope.launch {
            when (val state = providerInstallStates[provider.id] ?: ProviderInstallState.NotInstalled) {
                is ProviderInstallState.Installing -> onCancelDownload(state.downloadId, provider)
                is ProviderInstallState.Installed -> onUninstallProvider(provider)
                is ProviderInstallState.Outdated -> onUpdateProvider(provider)
                is ProviderInstallState.NotInstalled -> onInstallProvider(provider)
                else -> Unit
            }
        }
    }

    fun onUninstall(provider: ProviderMetadata) {
        val job = providerJobs[provider.id]
        if (job?.isActive == true || installSelectionJob?.isActive == true) return

        providerJobs[provider.id] = appDispatchers.ioScope.launch {
            onUninstallProvider(provider)
        }
    }

    fun onRepositoryFilterClick(repositoryUrl: String) {
        val filters = (_filters.value as? Async.Success)?.data ?: return
        val repositoryFilterIndex = filters.indexOfFirst { it is RepositoriesFilters }
        if (repositoryFilterIndex == -1) return

        val repositoryFilter = filters[repositoryFilterIndex] as RepositoriesFilters
        val formatted = extractGithubInfoFromLink(repositoryUrl)?.let { (owner, repo) ->
            String.format(Locale.getDefault(), REPOSITORY_NAME_OWNER_FORMAT, owner, repo)
        } ?: return
        onUpdateFilter(repositoryFilterIndex, repositoryFilter.copy(selectedValue = setOf(formatted)))
    }

    fun onUpdateFilter(index: Int, filter: AddProviderFilterType<*>) {
        _filters.update { asyncFilters ->
            if (asyncFilters !is Async.Success) return@update asyncFilters
            Async.Success(asyncFilters.data.set(index, filter))
        }
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
}

@Immutable
internal data class AddProviderUiState(
    val isInstallingProviders: Boolean = false,
    val repositoryExceptions: List<RepositoryWithError> = emptyList(),
    val providerExceptions: List<ProviderWithThrowable> = emptyList(),
    val isShowingSearchBar: Boolean = false,
)

internal typealias RepositoryWithError = Pair<Repository, UiText>

@Immutable
internal data class ProviderItem(
    val metadata: ProviderMetadata,
    val searchText: String,
) {
    val id get() = metadata.id
    val name get() = metadata.name

    companion object {
        fun from(provider: ProviderMetadata): ProviderItem {
            val searchText = buildString {
                append(provider.id)
                append(provider.name)
                provider.description?.let { append(it) }
                append(provider.providerType.type)
                append(provider.language.code)
                provider.authors.forEach { append(it) }

                extractGithubInfoFromLink(provider.repositoryUrl)?.let { (username, repository) ->
                    append(
                        String.format(
                            Locale.getDefault(),
                            REPOSITORY_NAME_OWNER_FORMAT,
                            username,
                            repository,
                        )
                    )
                }

                append(provider.versionName)
            }

            return ProviderItem(provider, searchText)
        }
    }
}
