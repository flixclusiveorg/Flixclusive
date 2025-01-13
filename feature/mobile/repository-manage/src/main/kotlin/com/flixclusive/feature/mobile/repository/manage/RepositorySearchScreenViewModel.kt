package com.flixclusive.feature.mobile.repository.manage

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.util.awaitFirst
import com.flixclusive.core.network.util.Resource
import com.flixclusive.domain.provider.GetRepositoryUseCase
import com.flixclusive.model.datastore.user.ProviderPreferences
import com.flixclusive.model.datastore.user.UserPreferences
import com.flixclusive.model.provider.Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class RepositorySearchScreenViewModel @Inject constructor(
    private val getRepositoryUseCase: GetRepositoryUseCase,
    private val dataStoreManager: DataStoreManager,
) : ViewModel() {
    val urlQuery = mutableStateOf("")
    val errorMessage = mutableStateOf<Resource.Failure?>(null)

    private var addJob: Job? = null
    private var removeJob: Job? = null

    val selectedRepositories = mutableStateListOf<Repository>()
    private val providerPreferencesAsFlow = dataStoreManager
        .getUserPrefs<ProviderPreferences>(UserPreferences.PROVIDER_PREFS_KEY)

    val repositories = providerPreferencesAsFlow
        .mapLatest { it.repositories }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = providerPreferencesAsFlow.awaitFirst().repositories
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
                    updateProviderPrefs {
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
            updateProviderPrefs { settings ->
                val newList = settings.repositories.toMutableList().apply {
                    removeAll(selectedRepositories)
                }

                settings.copy(repositories = newList.toList())
            }

            clearSelection()
        }
    }

    private suspend fun updateProviderPrefs(
        transform: suspend (ProviderPreferences) -> ProviderPreferences
    ) {
        dataStoreManager.updateUserPrefs<ProviderPreferences>(UserPreferences.PROVIDER_PREFS_KEY, transform)
    }
}
