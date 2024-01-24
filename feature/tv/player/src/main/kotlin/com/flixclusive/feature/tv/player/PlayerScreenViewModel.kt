package com.flixclusive.feature.tv.player

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flixclusive.core.datastore.AppSettingsManager
import com.flixclusive.core.ui.player.BasePlayerViewModel
import com.flixclusive.core.ui.player.FlixclusivePlayerManager
import com.flixclusive.core.ui.player.PlayerScreenNavArgs
import com.flixclusive.core.util.common.ui.UiText
import com.flixclusive.data.watch_history.WatchHistoryRepository
import com.flixclusive.domain.database.WatchTimeUpdaterUseCase
import com.flixclusive.domain.provider.SourceLinksProviderUseCase
import com.flixclusive.domain.tmdb.SeasonProviderUseCase
import com.flixclusive.feature.tv.player.di.ViewModelFactoryProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.EntryPointAccessors

@Composable
internal fun playerScreenViewModel(args: PlayerScreenNavArgs): PlayerScreenViewModel {
    val factory = EntryPointAccessors.fromActivity(
        LocalContext.current as Activity,
        ViewModelFactoryProvider::class.java
    ).playerScreenViewModelFactory()

    return viewModel(factory = PlayerScreenViewModel.provideFactory(factory, args))
}


class PlayerScreenViewModel @AssistedInject constructor(
    @Assisted args: PlayerScreenNavArgs,
    appSettingsManager: AppSettingsManager,
    sourceLinksProvider: SourceLinksProviderUseCase,
    watchHistoryRepository: WatchHistoryRepository,
    watchTimeUpdaterUseCase: WatchTimeUpdaterUseCase,
    seasonProvider: SeasonProviderUseCase,
    player: FlixclusivePlayerManager,
) : BasePlayerViewModel(
    args = args,
    watchHistoryRepository = watchHistoryRepository,
    appSettingsManager = appSettingsManager,
    seasonProviderUseCase = seasonProvider,
    sourceLinksProvider = sourceLinksProvider,
    watchTimeUpdaterUseCase = watchTimeUpdaterUseCase,
    player = player,
) {

    @AssistedFactory
    interface Factory {
        fun create(args: PlayerScreenNavArgs): PlayerScreenViewModel
    }

    @Suppress("UNCHECKED_CAST")
    companion object {
        fun provideFactory(
            assistedFactory: Factory,
            args: PlayerScreenNavArgs
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return assistedFactory.create(args) as T
            }
        }
    }

    override fun showErrorOnUiCallback(message: UiText) {

    }
}
