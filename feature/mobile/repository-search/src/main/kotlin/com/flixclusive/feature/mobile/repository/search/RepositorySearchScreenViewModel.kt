package com.flixclusive.feature.mobile.repository.search

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.datastore.AppSettingsManager
import com.flixclusive.core.network.util.Resource
import com.flixclusive.domain.provider.GetRepositoryUseCase
import com.flixclusive.model.provider.Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class RepositorySearchScreenViewModel @Inject constructor(
    private val getRepositoryUseCase: GetRepositoryUseCase,
    private val appSettingsManager: AppSettingsManager,
) : ViewModel() {
    val urlQuery = mutableStateOf("")
    val errorMessage = mutableStateOf<Resource.Failure?>(null)

    private var addJob: Job? = null
    private var removeJob: Job? = null

    val selectedRepositories = mutableStateListOf<Repository>()
    val repositories = appSettingsManager.providerSettings
        .data
        .map { it.repositories }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = appSettingsManager.cachedProviderSettings.repositories
        )

    fun selectRepository(repository: Repository) {
        selectedRepositories.add(repository)
    }

    fun unselectRepository(repository: Repository) {
        selectedRepositories.remove(repository)
    }

    fun clearSelection() {
        selectedRepositories.clear()
    }

    fun onAddLink() {
        if (addJob?.isActive == true)
            return

        addJob = viewModelScope.launch {
            errorMessage.value = null

            when (val repository = getRepositoryUseCase(urlQuery.value)) {
                is Resource.Failure -> errorMessage.value = repository
                Resource.Loading -> Unit
                is Resource.Success -> {
                    appSettingsManager.updateProviderSettings {
                        it.copy(repositories = it.repositories + repository.data!!)
                    }
                }
            }
        }
    }

    fun onRemoveRepositories() {
        if (removeJob?.isActive == true)
            return

        removeJob = viewModelScope.launch {
            appSettingsManager.updateProviderSettings { settings ->
                val newList = settings.repositories.toMutableList().apply {
                    removeAll(selectedRepositories)
                }

                settings.copy(repositories = newList.toList())
            }

            clearSelection()
        }
    }
}
