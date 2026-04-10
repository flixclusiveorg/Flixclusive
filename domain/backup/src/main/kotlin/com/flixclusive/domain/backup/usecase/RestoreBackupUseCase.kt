package com.flixclusive.domain.backup.usecase

import android.net.Uri
import com.flixclusive.domain.backup.common.BackupState
import kotlinx.coroutines.flow.Flow

interface RestoreBackupUseCase {
    operator fun invoke(
        uri: Uri,
    ): Flow<BackupState>
}
