package com.flixclusive.domain.backup.common

import com.flixclusive.data.backup.repository.BackupResult

sealed class BackupState {
    data object Idle : BackupState()

    data object Loading : BackupState()

    data class Success(val result: BackupResult) : BackupState()

    data class Error(val error: Throwable) : BackupState()
}
