package com.flixclusive.feature.mobile.repository.search

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.datastore.AppSettingsManager
import com.flixclusive.core.util.common.resource.Resource
import com.flixclusive.domain.provider.GetRepositoryUseCase
import com.flixclusive.feature.mobile.repository.search.util.extractGithubInfoFromLink
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.flixclusive.core.util.R as UtilR

@HiltViewModel
class RepositorySearchScreenViewModel @Inject constructor(
    private val getRepositoryUseCase: GetRepositoryUseCase,
    private val appSettingsManager: AppSettingsManager,
) : ViewModel() {
    val urlQuery = mutableStateOf("")
    val errorMessage = mutableStateOf<Resource.Failure?>(null)

    private var addJob: Job? = null

    val repositories = appSettingsManager.providerSettings
        .data
        .map { it.repositories }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = appSettingsManager.localProviderSettings.repositories
        )

    fun onAddLink() {
        if (addJob?.isActive == true)
            return

        addJob = viewModelScope.launch {
            val (username, repositoryName) = extractGithubInfoFromLink(urlQuery.value) ?: (null to null)
            val isAlreadyAdded = repositories.value.any { it.owner.equals(username, true) && it.name == repositoryName }

            if (isAlreadyAdded) {
                errorMessage.value = Resource.Failure(UtilR.string.already_added_repo_error)
                return@launch
            }

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
}
