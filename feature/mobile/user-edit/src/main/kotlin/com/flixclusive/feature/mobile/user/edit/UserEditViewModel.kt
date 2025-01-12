package com.flixclusive.feature.mobile.user.edit

import androidx.lifecycle.ViewModel
import com.flixclusive.core.locale.UiText
import com.flixclusive.core.util.coroutines.AppDispatchers.Companion.launchOnIO
import com.flixclusive.data.search_history.SearchHistoryRepository
import com.flixclusive.data.user.UserRepository
import com.flixclusive.data.watch_history.WatchHistoryRepository
import com.flixclusive.data.watchlist.WatchlistRepository
import com.flixclusive.model.database.User
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.flixclusive.core.locale.R as LocaleR

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
        override val name: UiText
    ) : Library()
}

@HiltViewModel
internal class UserEditViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val searchHistoryRepository: SearchHistoryRepository,
    private val watchlistRepository: WatchlistRepository,
    private val watchHistoryRepository: WatchHistoryRepository
) : ViewModel() {
    fun onRemoveUser(userId: Int) {
        launchOnIO {
            userRepository.deleteUser(userId)
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
        libraries: List<Library>
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
}
