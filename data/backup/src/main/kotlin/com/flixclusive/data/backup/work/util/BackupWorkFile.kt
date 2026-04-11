package com.flixclusive.data.backup.work.util

import com.flixclusive.core.common.file.AppStorage
import com.flixclusive.data.backup.repository.BackupResult
import kotlinx.serialization.json.Json
import java.io.File

internal object BackupWorkFile {
    fun getUserBackupDirectory(userId: Int): File {
        return File(
            AppStorage.getPublicBackupDirectory(),
            "user-$userId",
        )
    }

    fun getLastBackupResultFile(
        userId: Int,
        fileName: String
    ): File {
        return File(
            getUserBackupDirectory(userId),
            fileName,
        )
    }

    fun writeBackupResult(file: File, result: BackupResult) {
        file.parentFile?.mkdirs()
        file.writeText(Json.encodeToString(BackupResult.serializer(), result))
    }

    fun readBackupResult(
        userId: Int,
        fileName: String
    ): BackupResult {
        val file = getLastBackupResultFile(userId, fileName)
        if (!file.exists()) throw NoSuchFileException(file)

        val json = file.readText()
        return Json.decodeFromString(BackupResult.serializer(), json)
    }
}
