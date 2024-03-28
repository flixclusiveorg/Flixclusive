package com.flixclusive.feature.mobile.provider.settings

import androidx.lifecycle.ViewModel
import com.flixclusive.core.datastore.AppSettingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ProviderSettingsScreenViewModel @Inject constructor(
    private val appSettingsManager: AppSettingsManager,
) : ViewModel() {

}
