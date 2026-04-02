package com.flixclusive.feature.mobile.repository.manage

import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.database.entity.provider.InstalledRepository
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.core.network.util.Resource
import com.flixclusive.data.provider.repository.InstalledRepoRepository
import com.flixclusive.domain.provider.usecase.get.GetRepositoryUseCase
import com.flixclusive.domain.provider.util.extensions.toRepository
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
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
internal class RepositoryManagerViewModel @Inject constructor(
    private val getRepository: GetRepositoryUseCase,
    private val installedRepoRepository: InstalledRepoRepository,
    private val userSessionDataStore: UserSessionDataStore,
    private val appDispatchers: AppDispatchers,
) : ViewModel() {
    companion object {
        fun Repository.toInstalledRepository(userId: Int): InstalledRepository {
            return InstalledRepository(
                name = name,
                owner = owner,
                url = url,
                userId = userId,
                rawLinkFormat = rawLinkFormat,
            )
        }
    }

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

    val repositories = combine(
        userSessionDataStore.currentUserId.filterNotNull(),
        searchQuery.debounce(800)
    ) { userId, query ->
        userId to query
    }.flatMapLatest { (userId, query) ->
        installedRepoRepository.getAllAsFlow(userId).map { list ->
            list.fastFilter {
                it.name.contains(query, true) ||
                    it.owner.contains(query, true)
            }.fastMap { it.toRepository() }
        }
    }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
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
                    val userId = userSessionDataStore.currentUserId.filterNotNull().first()
                    installedRepoRepository.insert(
                        item = repository.data!!.toInstalledRepository(userId)
                    )
                }
            }
        }
    }

    fun onRemoveRepository(repository: Repository) {
        if (removeJob?.isActive == true) {
            return
        }

        removeJob = appDispatchers.ioScope.launch {
            val userId = userSessionDataStore.currentUserId.filterNotNull().first()
            installedRepoRepository.delete(repository.toInstalledRepository(userId))
        }
    }

    fun onRemoveSelection() {
        if (removeJob?.isActive == true) {
            return
        }

        removeJob = appDispatchers.ioScope.launch {
            _selectedRepositories.value.forEach { repository ->
                val userId = userSessionDataStore.currentUserId.filterNotNull().first()
                installedRepoRepository.delete(repository.toInstalledRepository(userId))
            }
        }
    }

    fun onConsumeError() {
        _uiState.update { it.copy(error = null) }
    }
}

internal data class RepositoryManagerUiState(
    val error: UiText? = null,
    val isShowingSearchBar: Boolean = false,
)
