package com.flixclusive.presentation.mobile.screens.preferences.providers

import androidx.lifecycle.ViewModel
import com.flixclusive.domain.usecase.ModifyProvidersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ProvidersListViewModel @Inject constructor(
    private val modifyProvidersUseCase: ModifyProvidersUseCase,
) : ViewModel() {
    val providers = modifyProvidersUseCase.availableProviders

    fun onMove(fromIndex: Int, toIndex: Int) = modifyProvidersUseCase.swap(fromIndex, toIndex)

    fun toggleProvider(index: Int) = modifyProvidersUseCase.toggleUsage(index)
}
