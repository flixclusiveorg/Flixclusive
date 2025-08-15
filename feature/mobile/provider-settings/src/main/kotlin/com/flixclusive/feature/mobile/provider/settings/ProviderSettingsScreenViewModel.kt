package com.flixclusive.feature.mobile.provider.settings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.flixclusive.core.ui.common.navigation.navargs.ProviderMetadataNavArgs
import com.flixclusive.domain.provider.repository.ProviderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
internal class ProviderSettingsScreenViewModel @Inject constructor(
    providerRepository: com.flixclusive.domain.provider.repository.ProviderRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val metadata = savedStateHandle.navArgs<ProviderMetadataNavArgs>().providerMetadata
    val providerInstance = providerRepository.getProvider(metadata.id)
}
