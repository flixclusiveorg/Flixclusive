package com.flixclusive.domain.backup.usecase

import android.net.Uri
import com.flixclusive.data.backup.model.BackupOptions
import com.flixclusive.domain.backup.common.BackupState
import kotlinx.coroutines.flow.Flow

interface RestoreBackupUseCase {
    operator fun invoke(
        uri: Uri,
        options: BackupOptions = BackupOptions(),
    ): Flow<BackupState>
}
