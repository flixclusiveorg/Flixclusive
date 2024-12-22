package com.flixclusive.feature.mobile.user.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.locale.UiText
import com.flixclusive.data.search_history.SearchHistoryRepository
import com.flixclusive.data.user.UserRepository
import com.flixclusive.model.database.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
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
    private val searchHistoryRepository: SearchHistoryRepository
) : ViewModel() {
    fun onRemoveUser(user: User) {
        viewModelScope.launch {
            userRepository.deleteUser(user.id)
        }
    }

    fun onEditUser(user: User) {
        viewModelScope.launch {
            userRepository.addUser(user)
        }
    }

    fun onClearSearchHistory(userId: Int) {
        viewModelScope.launch {
            searchHistoryRepository.clearAll(ownerId = userId)
        }
    }
}
