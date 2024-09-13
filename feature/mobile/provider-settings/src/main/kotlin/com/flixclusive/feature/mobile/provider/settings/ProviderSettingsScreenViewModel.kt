package com.flixclusive.feature.mobile.provider.settings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.flixclusive.core.ui.common.navigation.navargs.ProviderInfoScreenNavArgs
import com.flixclusive.data.provider.ProviderManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
internal class ProviderSettingsScreenViewModel @Inject constructor(
    providerManager: ProviderManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val providerData = savedStateHandle.navArgs<ProviderInfoScreenNavArgs>().providerData
    val providerInstance = providerManager.providers[providerData.name]
}
