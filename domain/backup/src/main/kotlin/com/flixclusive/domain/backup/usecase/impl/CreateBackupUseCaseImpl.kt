package com.flixclusive.domain.backup.usecase.impl

import android.net.Uri
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.data.backup.model.BackupOptions
import com.flixclusive.data.backup.repository.BackupRepository
import com.flixclusive.domain.backup.common.BackupState
import com.flixclusive.domain.backup.usecase.CreateBackupUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

internal class CreateBackupUseCaseImpl @Inject constructor(
    private val backupRepository: BackupRepository,
    private val appDispatchers: AppDispatchers
) : CreateBackupUseCase {
    override fun invoke(
        uri: Uri,
        options: BackupOptions
    ): Flow<BackupState> = flow {
        emit(BackupState.Loading)

        runCatching {
            backupRepository.create(
                uri = uri,
                options = options
            )
        }.onSuccess { result ->
            emit(BackupState.Success(result))
        }.onFailure { error ->
            emit(BackupState.Error(error))
        }
    }.flowOn(appDispatchers.io)
}
