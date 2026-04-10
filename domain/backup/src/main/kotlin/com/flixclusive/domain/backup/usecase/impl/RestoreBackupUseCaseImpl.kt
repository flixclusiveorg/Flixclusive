package com.flixclusive.domain.backup.usecase.impl

import android.net.Uri
import androidx.work.WorkInfo
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.core.datastore.model.user.BackupOptions
import com.flixclusive.data.backup.repository.BackupResult
import com.flixclusive.data.backup.work.BackupWorkManager
import com.flixclusive.domain.backup.common.BackupState
import com.flixclusive.domain.backup.usecase.RestoreBackupUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import javax.inject.Inject

internal class RestoreBackupUseCaseImpl @Inject constructor(
    private val appDispatchers: AppDispatchers,
    private val userSessionDataStore: UserSessionDataStore,
    private val backupWorkManager: BackupWorkManager,
) : RestoreBackupUseCase {
    override fun invoke(
        uri: Uri,
        options: BackupOptions,
    ): Flow<BackupState> = flow {
        emit(BackupState.Loading)

        val userId = userSessionDataStore.currentUserId.filterNotNull().first()
        val uniqueWorkName = backupWorkManager.enqueueRestore(
            userId = userId,
            uri = uri,
            options = options,
        )

        emitAll(
            backupWorkManager.observeUniqueWork(uniqueWorkName)
                .distinctUntilChangedBy { it?.state }
                .mapLatest { info ->
                    info.toBackupState(
                        userId = userId,
                        readResult = { backupWorkManager.readLastRestoreResult(userId) },
                    )
                }
                .distinctUntilChanged(),
        )
    }
        .catch { emit(BackupState.Error(it)) }
        .flowOn(appDispatchers.io)

    private suspend fun WorkInfo?.toBackupState(
        userId: Int,
        readResult: suspend () -> BackupResult,
    ): BackupState {
        val state = this?.state
        return when (state) {
            null,
            WorkInfo.State.ENQUEUED,
            WorkInfo.State.RUNNING,
            WorkInfo.State.BLOCKED,
            -> BackupState.Loading

            WorkInfo.State.SUCCEEDED -> {
                BackupState.Success(readResult())
            }

            WorkInfo.State.FAILED -> {
                val message = this.outputData
                    .getString(ERROR_MESSAGE_KEY)
                    ?: "Restore failed for user-$userId"
                BackupState.Error(IllegalStateException(message))
            }

            WorkInfo.State.CANCELLED -> {
                BackupState.Error(IllegalStateException("Restore cancelled for user-$userId"))
            }
        }
    }

    private companion object {
        private const val ERROR_MESSAGE_KEY = "errorMessage"
    }
}
