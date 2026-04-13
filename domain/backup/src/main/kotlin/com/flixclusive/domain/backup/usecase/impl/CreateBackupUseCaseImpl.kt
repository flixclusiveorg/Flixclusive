package com.flixclusive.domain.backup.usecase.impl

import android.net.Uri
import androidx.work.WorkInfo
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.core.datastore.model.user.BackupOptions
import com.flixclusive.data.backup.repository.BackupResult
import com.flixclusive.data.backup.work.BackupWorkManager
import com.flixclusive.domain.backup.common.BackupState
import com.flixclusive.domain.backup.usecase.CreateBackupUseCase
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

internal class CreateBackupUseCaseImpl @Inject constructor(
    private val userSessionDataStore: UserSessionDataStore,
    private val backupWorkManager: BackupWorkManager,
    private val appDispatchers: AppDispatchers,
) : CreateBackupUseCase {
    override fun invoke(): Flow<BackupState> = createBackupFlow {
        backupWorkManager.enqueueCreate(it)
    }

    override fun invoke(
        uri: Uri,
        options: BackupOptions,
    ): Flow<BackupState> = createBackupFlow { userId ->
        backupWorkManager.enqueueCreate(
            userId = userId,
            uri = uri,
            options = options,
        )
    }

    private fun createBackupFlow(
        enqueue: (userId: String) -> String,
    ): Flow<BackupState> = flow {
        emit(BackupState.Loading)

        val userId = userSessionDataStore.currentUserId.filterNotNull().first()
        val uniqueWorkName = enqueue(userId)

        emitAll(
            backupWorkManager.observeUniqueWork(uniqueWorkName)
                .distinctUntilChangedBy { it?.state }
                .mapLatest { info ->
                    info.toBackupState(
                        userId = userId,
                        readResult = { backupWorkManager.readLastCreateResult(userId) },
                    )
                }
                .distinctUntilChanged(),
        )
    }
        .catch { emit(BackupState.Error(it)) }
        .flowOn(appDispatchers.io)

    private suspend fun WorkInfo?.toBackupState(
        userId: String,
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
                    ?: "Backup failed for user-$userId"
                BackupState.Error(IllegalStateException(message))
            }

            WorkInfo.State.CANCELLED -> {
                BackupState.Error(IllegalStateException("Backup cancelled for user-$userId"))
            }
        }
    }

    private companion object {
        private const val ERROR_MESSAGE_KEY = "errorMessage"
    }
}
