package com.flixclusive.feature.mobile.update

import androidx.lifecycle.ViewModel
import com.flixclusive.domain.updater.AppUpdateCheckerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
internal class UpdateFeatureViewModel @Inject constructor(
    val appUpdateCheckerUseCase: AppUpdateCheckerUseCase
) : ViewModel()