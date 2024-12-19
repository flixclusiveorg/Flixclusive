package com.flixclusive.feature.mobile.user.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.data.user.UserRepository
import com.flixclusive.model.database.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class UserEditViewModel @Inject constructor(
    private val userRepository: UserRepository
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
}
