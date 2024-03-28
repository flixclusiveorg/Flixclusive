package com.flixclusive.feature.mobile.repository

import androidx.lifecycle.ViewModel
import com.flixclusive.core.datastore.AppSettingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class RepositoryScreenViewModel @Inject constructor(
    private val appSettingsManager: AppSettingsManager,
) : ViewModel() {

}
