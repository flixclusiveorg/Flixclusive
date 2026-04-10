package com.flixclusive.data.backup.work.util

import android.content.Context
import com.flixclusive.core.common.file.AppStorage
import java.io.File

internal object BackupWorkFiles {
    fun getUserBackupDirectory(userId: Int): File {
        return File(
            AppStorage.getPublicBackupDirectory(),
            "user-$userId",
        )
    }

    fun getLastBackupResultFile(userId: Int): File {
        return File(
            getUserBackupDirectory(userId),
            BackupWorkConstants.LAST_RESULT_FILE_NAME,
        )
    }

    fun getLastRestoreResultFile(context: Context, userId: Int): File {
        return File(
            File(context.cacheDir, "backup/restore/user-$userId"),
            BackupWorkConstants.LAST_RESTORE_RESULT_FILE_NAME,
        )
    }
}
