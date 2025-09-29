package com.flixclusive.feature.mobile.user.edit

import androidx.lifecycle.ViewModel
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.database.entity.user.User
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.data.database.repository.SearchHistoryRepository
import com.flixclusive.data.database.repository.UserRepository
import com.flixclusive.data.database.repository.WatchProgressRepository
import com.flixclusive.data.database.repository.WatchlistRepository
import com.flixclusive.data.database.session.UserSessionManager
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.domain.provider.usecase.manage.UnloadProviderUseCase
import com.flixclusive.feature.mobile.user.edit.OnRemoveNavigationState.Companion.getStateIfUserIsLoggedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.flixclusive.core.strings.R as LocaleR

@HiltViewModel
internal class UserEditViewModel
    @Inject
    constructor(
        private val dataStoreManager: DataStoreManager,
        private val userRepository: UserRepository,
        private val userSessionManager: UserSessionManager,
        private val searchHistoryRepository: SearchHistoryRepository,
        private val watchlistRepository: WatchlistRepository,
        private val watchProgressRepository: WatchProgressRepository,
        private val providerRepository: ProviderRepository,
        private val unloadProvider: UnloadProviderUseCase,
        private val appDispatchers: AppDispatchers,
    ) : ViewModel() {
        private val _onRemoveNavigationState = MutableSharedFlow<OnRemoveNavigationState>()
        val onRemoveNavigationState = _onRemoveNavigationState.asSharedFlow()

        private var removeUserJob: Job? = null
        private var editUserJob: Job? = null
        private var clearSearchHistoryJob: Job? = null
        private var clearLibrariesJob: Job? = null

        fun onRemoveUser(userId: Int) {
            if (removeUserJob?.isActive == true) return

            removeUserJob = appDispatchers.ioScope.launch {
                val isUserLoggedIn = isUserLoggedIn(userId)
                val navigationState = getStateIfUserIsLoggedIn(isUserLoggedIn)

                // CLEAR PREFS
                clearProviders(userId)
                signOut(userId)
                dataStoreManager.deleteAllUserRelatedFiles(userId)
                // ===

                // CLEAR DATABASE
                searchHistoryRepository.clearAll(userId)
                watchProgressRepository.removeAll(userId)
                watchlistRepository.removeAll(userId)
                userRepository.deleteUser(userId)
                // ===

                _onRemoveNavigationState.emit(navigationState)
            }
        }

        fun onEditUser(user: User) {
            if (editUserJob?.isActive == true) return

            editUserJob = appDispatchers.ioScope.launch {
                userRepository.updateUser(user)
            }
        }

        fun onClearSearchHistory(userId: Int) {
            if (clearSearchHistoryJob?.isActive == true) return

            clearSearchHistoryJob = appDispatchers.ioScope.launch {
                searchHistoryRepository.clearAll(ownerId = userId)
            }
        }

        fun onClearLibraries(
            userId: Int,
            libraries: List<Library>,
        ) {
            if (clearLibrariesJob?.isActive == true) return

            clearLibrariesJob = appDispatchers.ioScope.launch {
                libraries.forEach {
                    when (it) {
                        is Library.Watchlist -> watchlistRepository.removeAll(userId)
                        is Library.WatchHistory -> watchProgressRepository.removeAll(userId)
                        is Library.CustomList -> throw IllegalStateException("Custom libraries are not yet implemented")
                    }
                }
            }
        }

        private suspend fun clearProviders(userId: Int) {
            if (!isUserLoggedIn(userId)) return
            providerRepository.getProviders().forEach {
                unloadProvider(it)
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
