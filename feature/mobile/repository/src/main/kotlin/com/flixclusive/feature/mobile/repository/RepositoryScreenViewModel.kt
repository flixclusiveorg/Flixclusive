package com.flixclusive.feature.mobile.repository

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.ui.mobile.component.provider.ProviderCardState
import com.flixclusive.core.util.common.resource.Resource
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.data.provider.ProviderManager
import com.flixclusive.domain.provider.GetOnlineProvidersUseCase
import com.flixclusive.gradle.entities.ProviderData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RepositoryScreenViewModel @Inject constructor(
    private val providerManager: ProviderManager,
    private val getOnlineProvidersUseCase: GetOnlineProvidersUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    val repository = savedStateHandle.navArgs<RepositoryScreenNavArgs>().repository

    var uiState by mutableStateOf<Resource<List<ProviderData>>>(Resource.Loading)
        private set

    var searchQuery by mutableStateOf("")
        private set
    val onlineProviderMap = mutableStateMapOf<ProviderData, ProviderCardState>()

    private var initJob: Job? = null

    init {
        initialize()
    }

    fun initialize() {
        if (initJob?.isActive == true) {
            return
        }

        initJob = viewModelScope.launch {
            try {
                uiState = Resource.Loading

                val onlineProviders = getOnlineProvidersUseCase(repository)
                onlineProviderMap.clear()

                uiState = onlineProviders
                if (onlineProviders is Resource.Success) {
                    onlineProviders.data!!.forEach { provider ->
                        var providerCardState = ProviderCardState.NotInstalled

                        if (providerManager.providerDataMap[provider.name] != null) {
                            providerCardState = ProviderCardState.Installed
                        }

                        onlineProviderMap[provider] = providerCardState
                    }
                }
            } catch (e: Exception) {
                errorLog(e.stackTraceToString())
                uiState = Resource.Failure(e.localizedMessage)
            }
        }
    }

    fun installAll() {
        onlineProviderMap.forEach { data, state ->
            TODO("Not yet implemented")
        }
    }

    fun toggleProvider(
        providerData: ProviderData
    ) {
        val isUninstalling = onlineProviderMap[providerData] == ProviderCardState.Installed
        TODO("Not yet implemented")
    }

    fun onSearchQueryChange(newQuery: String) {
        searchQuery = newQuery
    }
}
