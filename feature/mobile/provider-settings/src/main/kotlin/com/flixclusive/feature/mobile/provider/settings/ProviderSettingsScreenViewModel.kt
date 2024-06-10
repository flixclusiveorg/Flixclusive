package com.flixclusive.feature.mobile.provider.settings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.flixclusive.core.ui.common.navigation.ProviderInfoScreenNavArgs
import com.flixclusive.data.provider.ProviderManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ProviderSettingsScreenViewModel @Inject constructor(
    providerManager: ProviderManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    val providerData = savedStateHandle.navArgs<ProviderInfoScreenNavArgs>().providerData
    val providerInstance = providerManager.providers[providerData.name]
}
