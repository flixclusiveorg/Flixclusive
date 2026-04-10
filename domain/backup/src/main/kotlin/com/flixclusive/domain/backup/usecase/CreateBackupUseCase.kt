package com.flixclusive.domain.backup.usecase

import com.flixclusive.domain.backup.common.BackupState
import kotlinx.coroutines.flow.Flow

interface CreateBackupUseCase {
    operator fun invoke(): Flow<BackupState>
}
