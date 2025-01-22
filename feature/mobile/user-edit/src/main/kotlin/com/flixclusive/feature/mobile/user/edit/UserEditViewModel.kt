package com.flixclusive.feature.mobile.user.edit

import androidx.lifecycle.ViewModel
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.locale.UiText
import com.flixclusive.core.util.coroutines.AppDispatchers.Companion.launchOnIO
import com.flixclusive.data.provider.ProviderRepository
import com.flixclusive.data.search.SearchHistoryRepository
import com.flixclusive.data.user.UserRepository
import com.flixclusive.data.library.recent.WatchHistoryRepository
import com.flixclusive.data.library.watchlist.WatchlistRepository
import com.flixclusive.domain.provider.ProviderUnloaderUseCase
import com.flixclusive.domain.user.UserSessionManager
import com.flixclusive.feature.mobile.user.edit.OnRemoveNavigationState.Companion.getStateIfUserIsLoggedIn
import com.flixclusive.model.database.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import com.flixclusive.core.locale.R as LocaleR

@HiltViewModel
internal class UserEditViewModel
    @Inject
    constructor(
        private val dataStoreManager: DataStoreManager,
        private val userRepository: UserRepository,
        private val userSessionManager: UserSessionManager,
        private val searchHistoryRepository: SearchHistoryRepository,
        private val watchlistRepository: WatchlistRepository,
        private val watchHistoryRepository: WatchHistoryRepository,
        private val providerRepository: ProviderRepository,
        private val providerUnloaderUseCase: ProviderUnloaderUseCase,
    ) : ViewModel() {
        private val _onRemoveNavigationState = MutableSharedFlow<OnRemoveNavigationState>()
        val onRemoveNavigationState = _onRemoveNavigationState.asSharedFlow()

        fun onRemoveUser(userId: Int) {
            launchOnIO {
                val isUserLoggedIn = isUserLoggedIn(userId)
                val navigationState = getStateIfUserIsLoggedIn(isUserLoggedIn)

                // CLEAR PREFS
                clearProviders(userId)
                signOut(userId)
                dataStoreManager.deleteAllUserRelatedFiles(userId)
                // ===

                // CLEAR DATABASE
                searchHistoryRepository.clearAll(userId)
                watchHistoryRepository.removeAll(userId)
                watchlistRepository.removeAll(userId)
                userRepository.deleteUser(userId)
                // ===

                _onRemoveNavigationState.emit(navigationState)
            }
        }

        fun onEditUser(user: User) {
            launchOnIO {
                userRepository.updateUser(user)
            }
        }

        fun onClearSearchHistory(userId: Int) {
            launchOnIO {
                searchHistoryRepository.clearAll(ownerId = userId)
            }
        }

        fun onClearLibraries(
            userId: Int,
            libraries: List<Library>,
        ) {
            launchOnIO {
                libraries.forEach {
                    when (it) {
                        is Library.Watchlist -> watchlistRepository.removeAll(userId)
                        is Library.WatchHistory -> watchHistoryRepository.removeAll(userId)
                        is Library.CustomList -> throw IllegalStateException("Custom libraries are not yet implemented")
                    }
                }
            }
        }

        private suspend fun clearProviders(userId: Int) {
            if (!isUserLoggedIn(userId)) return
            providerRepository.getProviders().forEach {
                providerUnloaderUseCase.unload(it)
            }
        }

        private suspend fun signOut(userId: Int) {
            if (!isUserLoggedIn(userId)) return
            userSessionManager.signOut()
        }

        private fun isUserLoggedIn(userId: Int): Boolean {
            return userSessionManager.currentUser.value?.id == userId
        }
    }

internal enum class OnRemoveNavigationState {
    PopToRoot,
    GoBack,
    ;

    companion object {
        fun getStateIfUserIsLoggedIn(isLoggedIn: Boolean): OnRemoveNavigationState {
            return when (isLoggedIn) {
                true -> PopToRoot
                false -> GoBack
            }
        }
    }
}

internal sealed class Library {
    abstract val name: UiText

    data object Watchlist : Library() {
        override val name: UiText = UiText.StringResource(LocaleR.string.watchlist)
    }

    data object WatchHistory : Library() {
        override val name: UiText = UiText.StringResource(LocaleR.string.recently_watched)
    }

    data class CustomList(
        val id: Int,
        override val name: UiText,
    ) : Library()
}
