package com.flixclusive.feature.mobile.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.model.system.SystemPreferences
import com.flixclusive.data.database.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

internal enum class NextStepNavigation {
    CONTINUE_ONBOARDING,
    HOME
}

@HiltViewModel
internal class OnboardingViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val dataStoreManager: DataStoreManager,
    private val appDispatchers: AppDispatchers,
) : ViewModel() {
    private val _nextStepNavigation = MutableSharedFlow<NextStepNavigation>()
    val nextStepNavigation = _nextStepNavigation.asSharedFlow()

    val systemPreferences = dataStoreManager
        .getSystemPrefs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SystemPreferences(),
        )

    fun updateStorageDirectoryUri(uri: String?) {
        appDispatchers.ioScope.launch {
            dataStoreManager.updateSystemPrefs {
                it.copy(storageDirectoryUri = uri)
            }
        }
    }

    fun completeOnboarding() {
        appDispatchers.ioScope.launch {
            dataStoreManager.updateSystemPrefs {
                it.copy(isFirstTimeUserLaunch = false)
            }

            if (userRepository.getAll().isEmpty()) {
                _nextStepNavigation.emit(NextStepNavigation.CONTINUE_ONBOARDING)
            } else {
                _nextStepNavigation.emit(NextStepNavigation.HOME)
            }
        }
    }
}
