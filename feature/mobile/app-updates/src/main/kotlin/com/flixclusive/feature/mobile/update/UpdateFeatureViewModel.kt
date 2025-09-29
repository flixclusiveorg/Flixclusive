package com.flixclusive.feature.mobile.update

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
internal class UpdateFeatureViewModel @Inject constructor(
    private val appUpdatesRepository: AppUpdatesRepository,
) : ViewModel()
