package com.flixclusive.feature.mobile.provider.settings

import androidx.compose.runtime.Stable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.core.navigation.navargs.ProviderMetadataNavArgs
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.provider.ProviderPlugin
import com.ramcosta.composedestinations.generated.providersettings.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

// TODO: Create a unit test if needed
@Stable
@HiltViewModel
internal class ProviderSettingsScreenViewModel @Inject constructor(
    private val userSessionDataStore: UserSessionDataStore,
    providerRepository: ProviderRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val metadata = savedStateHandle.navArgs<ProviderMetadataNavArgs>().metadata

    var providerPlugin: ProviderPlugin? = null
        private set

    init {
        viewModelScope.launch {
            val userId = userSessionDataStore.currentUserId.filterNotNull().first()
            val provider = providerRepository.getProvider(metadata.id, userId)
            providerPlugin = provider?.plugin
        }
    }
}
