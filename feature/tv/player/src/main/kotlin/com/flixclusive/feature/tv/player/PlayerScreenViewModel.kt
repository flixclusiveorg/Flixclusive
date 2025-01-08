package com.flixclusive.feature.tv.player

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.locale.UiText
import com.flixclusive.core.ui.player.BasePlayerViewModel
import com.flixclusive.core.ui.player.PlayerScreenNavArgs
import com.flixclusive.core.ui.player.util.PlayerCacheManager
import com.flixclusive.data.provider.ProviderRepository
import com.flixclusive.data.watch_history.WatchHistoryRepository
import com.flixclusive.domain.database.WatchTimeUpdaterUseCase
import com.flixclusive.domain.provider.GetMediaLinksUseCase
import com.flixclusive.domain.tmdb.SeasonProviderUseCase
import com.flixclusive.domain.user.UserSessionManager
import com.flixclusive.feature.tv.player.di.ViewModelFactoryProvider
import com.flixclusive.model.datastore.user.UserPreferences
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

@Composable
internal fun playerScreenViewModel(args: PlayerScreenNavArgs): PlayerScreenViewModel {
    val factory =
        EntryPointAccessors
            .fromActivity(
                LocalContext.current as Activity,
                ViewModelFactoryProvider::class.java,
            ).playerScreenViewModelFactory()

    return viewModel(factory = PlayerScreenViewModel.provideFactory(factory, args))
}

internal class PlayerScreenViewModel
    @AssistedInject
    constructor(
        @Assisted args: PlayerScreenNavArgs,
        private val dataStoreManager: DataStoreManager,
        client: OkHttpClient,
        context: Context,
        playerCacheManager: PlayerCacheManager,
        seasonProvider: SeasonProviderUseCase,
        getMediaLinksUseCase: GetMediaLinksUseCase,
        watchHistoryRepository: WatchHistoryRepository,
        watchTimeUpdaterUseCase: WatchTimeUpdaterUseCase,
        providerRepository: ProviderRepository,
        userSessionManager: UserSessionManager,
    ) : BasePlayerViewModel(
            dataStoreManager = dataStoreManager,
            args = args,
            client = client,
            context = context,
            playerCacheManager = playerCacheManager,
            seasonProviderUseCase = seasonProvider,
            getMediaLinksUseCase = getMediaLinksUseCase,
            watchHistoryRepository = watchHistoryRepository,
            watchTimeUpdaterUseCase = watchTimeUpdaterUseCase,
            userSessionManager = userSessionManager,
            providerRepository = providerRepository,
        ) {
        @AssistedFactory
        interface Factory {
            fun create(args: PlayerScreenNavArgs): PlayerScreenViewModel
        }

        @Suppress("UNCHECKED_CAST")
        companion object {
            fun provideFactory(
                assistedFactory: Factory,
                args: PlayerScreenNavArgs,
            ): ViewModelProvider.Factory =
                object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T = assistedFactory.create(args) as T
                }
        }

        override fun showErrorSnackbar(
            message: UiText,
            isInternalPlayerError: Boolean,
        ) {}

        /**
         *
         * Used for subtitle style updates.
         * */
        inline fun <reified T : UserPreferences> updatePreferences(
            key: Preferences.Key<String>,
            newPreferences: T,
        ) {
            viewModelScope.launch {
                dataStoreManager.updateUserPrefs<T>(key) { newPreferences }
            }
        }
    }
