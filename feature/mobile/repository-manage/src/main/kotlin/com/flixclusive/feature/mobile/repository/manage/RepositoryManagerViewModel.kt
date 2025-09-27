package com.flixclusive.feature.mobile.repository.manage

import androidx.compose.ui.util.fastFilter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.model.user.ProviderPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.core.network.util.Resource
import com.flixclusive.domain.provider.usecase.get.GetRepositoryUseCase
import com.flixclusive.model.provider.Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
internal class RepositoryManagerViewModel
    @Inject
    constructor(
        private val getRepository: GetRepositoryUseCase,
        private val dataStoreManager: DataStoreManager,
        private val appDispatchers: AppDispatchers,
    ) : ViewModel() {
        private var addJob: Job? = null
        private var removeJob: Job? = null

        private val _uiState = MutableStateFlow(RepositoryManagerUiState())
        val uiState = _uiState.asStateFlow()

        private val _searchQuery = MutableStateFlow("")
        val searchQuery = _searchQuery.asStateFlow()

        private val _urlQuery = MutableStateFlow("")
        val urlQuery = _urlQuery.asStateFlow()

        private val _selectedRepositories = MutableStateFlow(persistentSetOf<Repository>())
        val selectedRepositories = _selectedRepositories.asStateFlow()

        val repositories = dataStoreManager
            .getUserPrefs(UserPreferences.PROVIDER_PREFS_KEY, ProviderPreferences::class)
            .mapLatest { it.repositories }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = emptyList(),
            )

        val searchResults = searchQuery
            .debounce(800)
            .combine(repositories) { query, repositories ->
                repositories.fastFilter {
                    it.name.contains(query, true) ||
                        it.owner.contains(query, true)
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = emptyList(),
            )

        fun toggleRepositorySelection(repository: Repository) {
            _selectedRepositories.update {
                if (it.contains(repository)) {
                    it.remove(repository)
                } else {
                    it.add(repository)
                }
            }
        }

        fun clearSelection() {
            _selectedRepositories.update { it.clear() }
        }

        fun onUrlQueryChange(query: String) {
            _urlQuery.value = query
        }

        fun onSearchQueryChange(query: String) {
            _searchQuery.value = query
        }

        fun onToggleSearchBar(state: Boolean) {
            _uiState.update {
                it.copy(isShowingSearchBar = state)
            }
        }

        fun onAddLink() {
            if (addJob?.isActive == true) {
                return
            }

            addJob = appDispatchers.ioScope.launch {
                when (val repository = getRepository(_urlQuery.value)) {
                    Resource.Loading -> Unit
                    is Resource.Failure -> {
                        _uiState.update { it.copy(error = repository.error) }
                    }

                    is Resource.Success -> {
                        updateProviderPrefs {
                            it.copy(repositories = it.repositories + repository.data!!)
                        }
                    }
                }
            }
        }

        fun onRemoveRepository(repository: Repository) {
            if (removeJob?.isActive == true) {
                return
            }

            removeJob = appDispatchers.ioScope.launch {
                updateProviderPrefs { settings ->
                    val newList = settings.repositories.toMutableList()
                    newList.remove(repository)
                    settings.copy(repositories = newList.toList())
                }
            }
        }

        fun onRemoveSelection() {
            if (removeJob?.isActive == true) {
                return
            }

            removeJob = appDispatchers.ioScope.launch {
                _selectedRepositories.value.forEach { repository ->
                    updateProviderPrefs { settings ->
                        val newList = settings.repositories.toMutableList()
                        newList.remove(repository)
                        settings.copy(repositories = newList.toList())
                    }
                }
        }
    }

    fun onConsumeError() {
        _uiState.update { it.copy(error = null) }
        }

    private suspend fun updateProviderPrefs(transform: suspend (ProviderPreferences) -> ProviderPreferences) {
        dataStoreManager.updateUserPrefs<ProviderPreferences>(
            UserPreferences.PROVIDER_PREFS_KEY,
            ProviderPreferences::class,
            transform,
            )
        }
    }

internal data class RepositoryManagerUiState(
    val error: UiText? = null,
    val isShowingSearchBar: Boolean = false,
)
