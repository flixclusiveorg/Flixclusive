package com.flixclusive.feature.tv.player.di

import com.flixclusive.feature.tv.player.PlayerScreenViewModel
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent

@EntryPoint
@InstallIn(ActivityComponent::class)
internal interface ViewModelFactoryProvider {
    fun playerScreenViewModelFactory(): PlayerScreenViewModel.Factory
}