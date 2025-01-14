package com.flixclusive.feature.mobile.repository.manage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.util.coroutines.AppDispatchers
import com.flixclusive.core.util.coroutines.asStateFlow
import com.flixclusive.domain.provider.GetRepositoryUseCase
import com.flixclusive.model.datastore.user.ProviderPreferences
import com.flixclusive.model.datastore.user.UserPreferences
import com.flixclusive.model.provider.Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class RepositoryManagerViewModel @Inject constructor(
    private val getRepositoryUseCase: GetRepositoryUseCase,
    private val dataStoreManager: DataStoreManager,
) : ViewModel() {
    private val _uiState = MutableStateFlow(RepositoryManagerUiState())
    val uiState = _uiState.asStateFlow()

    val selectedRepositories = uiState
        .mapLatest { it.selectedRepositories }
        .distinctUntilChanged()
        .asStateFlow(viewModelScope)

    private var addJob: Job? = null
    private var removeJob: Job? = null

    private val repositoriesAsFlow = dataStoreManager
        .getUserPrefs<ProviderPreferences>(UserPreferences.PROVIDER_PREFS_KEY)
        .mapLatest { it.repositories }
        .asStateFlow(viewModelScope)

    init {
        viewModelScope.launch {
            repositoriesAsFlow.collectLatest { repositories ->
                _uiState.update {
                    it.copy(repositories = repositories)
                }
            }
        }
    }

    fun selectRepository(repository: Repository) {
        _uiState.update {
            it.copy(selectedRepositories = it.selectedRepositories + repository)
        }
    }

    fun unselectRepository(repository: Repository) {
        _uiState.update {
            val newList = it.selectedRepositories.toMutableList()
            newList.remove(repository)
            it.copy(selectedRepositories = newList.toList())
        }
    }

    fun clearSelection() {
        _uiState.update {
            it.copy(selectedRepositories = emptyList())
        }
    }

    fun onUrlQueryChange(query: String) {
        _uiState.update {
            it.copy(urlQuery = query)
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update {
            it.copy(searchQuery = query)
        }
    }

    fun onToggleSearchBar(state: Boolean) {
        _uiState.update {
            it.copy(isShowingSearchBar = state)
        }
    }

    fun onAddLink() {
        if (addJob?.isActive == true)
            return

        addJob = AppDispatchers.IO.scope.launch {
            val repositoryUrl = _uiState.value.urlQuery
            when (val repository = getRepositoryUseCase(repositoryUrl)) {
                Resource.Loading -> Unit
                is Resource.Failure -> {
                    _uiState.update { it.copy(error = repository) }
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
        if (removeJob?.isActive == true)
            return

        removeJob = AppDispatchers.IO.scope.launch {
            updateProviderPrefs { settings ->
                val newList = settings.repositories.toMutableList()
                newList.remove(repository)
                settings.copy(repositories = newList.toList())
            }
        }
    }

    fun onRemoveRepositories() {
        if (removeJob?.isActive == true)
            return

        removeJob = AppDispatchers.IO.scope.launch {
            updateProviderPrefs { settings ->
                val newList = settings.repositories.toMutableList()
                newList.removeAll(_uiState.value.selectedRepositories)
                settings.copy(repositories = newList.toList())
            }

            clearSelection()
        }
    }

    fun onConsumeError() {
        _uiState.update { it.copy(error = null) }
    }

    fun onToggleAlertDialog(state: Boolean, repositoryToRemove: Repository? = null) {
        _uiState.update {
            it.copy(
                isShowingAlertDialog = state,
                singleRepositoryToRemove = repositoryToRemove
            )
        }
    }

    fun onInitializeFocus() {
        _uiState.update { it.copy(isFocusedInitialized = true) }
    }

    private suspend fun updateProviderPrefs(
        transform: suspend (ProviderPreferences) -> ProviderPreferences
    ) {
        dataStoreManager.updateUserPrefs<ProviderPreferences>(UserPreferences.PROVIDER_PREFS_KEY, transform)
    }
}

internal data class RepositoryManagerUiState(
    val repositories: List<Repository> = emptyList(),
    val selectedRepositories: List<Repository> = emptyList(),
    val singleRepositoryToRemove: Repository? = null,
    val error: Resource.Failure? = null,
    val urlQuery: String = "",
    val searchQuery: String = "",
    val isShowingSearchBar: Boolean = false,
    val isShowingAlertDialog: Boolean = false,
    val isFocusedInitialized: Boolean = false,
)
