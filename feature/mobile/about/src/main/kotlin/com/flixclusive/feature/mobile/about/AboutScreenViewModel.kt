package com.flixclusive.feature.mobile.about

import androidx.lifecycle.ViewModel
import com.flixclusive.data.configuration.AppConfigurationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AboutScreenViewModel @Inject constructor(
    appConfigurationManager: AppConfigurationManager
) : ViewModel() {
    val currentAppBuild = appConfigurationManager.currentAppBuild
}