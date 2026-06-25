package com.flixclusive.feature.mobile.settings.screen.root

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.common.config.BuildConfigProvider
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.data.database.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
internal class SettingsViewModel @Inject constructor(
    private val userSessionDataStore: UserSessionDataStore,
    private val _buildConfig: BuildConfigProvider,
    private val userRepository: UserRepository,
) : ViewModel() {
    val currentUser = userSessionDataStore.currentUserId
        .filterNotNull()
        .flatMapLatest { userId ->
            userRepository
                .observeUser(id = userId)
                .filterNotNull()
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = runBlocking {
                val userId = userSessionDataStore.currentUserId.filterNotNull().first()
                userRepository.getUser(id = userId)
                    ?: throw IllegalStateException("You cannot use the settings screen without being logged in")
            },
        )

    @Stable
    val buildConfig get() = _buildConfig.get()
}
