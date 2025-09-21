package com.flixclusive.feature.mobile.provider.settings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.flixclusive.core.navigation.navargs.ProviderMetadataNavArgs
import com.flixclusive.data.provider.repository.ProviderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

// TODO: Create a unit test if needed
@HiltViewModel
internal class ProviderSettingsScreenViewModel @Inject constructor(
    providerRepository: ProviderRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val metadata = savedStateHandle.navArgs<ProviderMetadataNavArgs>().metadata

    val providerInstance = providerRepository.getProvider(metadata.id)
}
