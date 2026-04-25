package com.flixclusive.feature.mobile.provider.settings

import androidx.compose.runtime.Stable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.navigation.navargs.ProviderMetadataNavArgs
import com.flixclusive.domain.provider.usecase.get.GetProviderPluginUseCase
import com.flixclusive.provider.ProviderPlugin
import com.ramcosta.composedestinations.generated.providersettings.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

// TODO: Create a unit test if needed
@Stable
@HiltViewModel
internal class ProviderSettingsScreenViewModel @Inject constructor(
    private val getProviderPlugin: GetProviderPluginUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val metadata = savedStateHandle.navArgs<ProviderMetadataNavArgs>().metadata

    var providerPlugin: ProviderPlugin? = null
        private set

    init {
        viewModelScope.launch {
            providerPlugin = getProviderPlugin(metadata.id)
        }
    }
}
