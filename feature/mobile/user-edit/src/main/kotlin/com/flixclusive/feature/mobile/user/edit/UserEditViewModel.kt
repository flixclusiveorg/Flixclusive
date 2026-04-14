package com.flixclusive.feature.mobile.user.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.database.entity.user.User
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.data.database.repository.LibraryListRepository
import com.flixclusive.data.database.repository.SearchHistoryRepository
import com.flixclusive.data.database.repository.UserRepository
import com.flixclusive.data.database.repository.WatchProgressRepository
import com.flixclusive.data.database.session.UserSessionManager
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.domain.provider.usecase.manage.UnloadProviderUseCase
import com.flixclusive.feature.mobile.user.R
import com.flixclusive.feature.mobile.user.edit.OnRemoveNavigationState.Companion.getStateIfUserIsLoggedIn
import com.ramcosta.composedestinations.generated.useredit.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.flixclusive.core.database.R as DatabaseR

@HiltViewModel
internal class UserEditViewModel @Inject constructor(
    private val dataStoreManager: DataStoreManager,
    private val userRepository: UserRepository,
    private val userSessionManager: UserSessionManager,
    private val searchHistoryRepository: SearchHistoryRepository,
    private val watchProgressRepository: WatchProgressRepository,
    private val libraryListRepository: LibraryListRepository,
    private val providerRepository: ProviderRepository,
    private val unloadProvider: UnloadProviderUseCase,
    private val appDispatchers: AppDispatchers,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val navArgs = savedStateHandle.navArgs<UserEditScreenNavArgs>()

    val user = userRepository.observeUser(navArgs.userId)
        .filterNotNull()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = User.Empty,
        )

    private val _onRemoveNavigationState = MutableSharedFlow<OnRemoveNavigationState>()
    val onRemoveNavigationState = _onRemoveNavigationState.asSharedFlow()

    private var removeUserJob: Job? = null
    private var editUserJob: Job? = null
    private var clearSearchHistoryJob: Job? = null
    private var clearLibrariesJob: Job? = null

    fun onRemoveUser(userId: String) {
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
            watchProgressRepository.deleteAll(userId)
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

    fun onClearSearchHistory(userId: String) {
        if (clearSearchHistoryJob?.isActive == true) return

        clearSearchHistoryJob = appDispatchers.ioScope.launch {
            searchHistoryRepository.clearAll(ownerId = userId)
        }
    }

    fun onClearLibraries(
        userId: String,
        libraries: List<Library>,
    ) {
        if (clearLibrariesJob?.isActive == true) return

        clearLibrariesJob = appDispatchers.ioScope.launch {
            libraries.forEach {
                when (it) {
                    is Library.WatchHistory -> watchProgressRepository.deleteAll(ownerId = userId)
                    is Library.CustomList -> libraryListRepository.deleteAllExceptWatched(ownerId = userId)
                }
            }
        }
    }

    private suspend fun clearProviders(userId: String) {
        if (!isUserLoggedIn(userId)) return
        providerRepository.getInstalledProviders(userId).forEach {
            unloadProvider(it)
        }
    }

    private suspend fun signOut(userId: String) {
        if (!isUserLoggedIn(userId)) return
        userSessionManager.signOut()
    }

    private fun isUserLoggedIn(userId: String): Boolean {
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

    data object WatchHistory : Library() {
        override val name: UiText = UiText.from(DatabaseR.string.seeded_recently_watched)
    }

    data object CustomList : Library() {
        override val name: UiText = UiText.from(R.string.custom_list)
    }
}
