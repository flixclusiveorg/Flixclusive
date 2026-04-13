package com.flixclusive.data.backup.work.util

import android.content.Context
import com.flixclusive.data.backup.repository.BackupResult
import kotlinx.serialization.json.Json
import java.io.File

internal object BackupWorkResultStore {
    private const val ROOT_FOLDER = "backup-work"

    private fun getUserFolder(context: Context, userId: Int): File {
        return File(
            File(context.filesDir, ROOT_FOLDER),
            "user-$userId",
        )
    }

    fun write(
        context: Context,
        userId: Int,
        fileName: String,
        result: BackupResult,
    ) {
        val file = File(getUserFolder(context, userId), fileName)
        file.parentFile?.mkdirs()
        file.writeText(Json.encodeToString(BackupResult.serializer(), result))
    }

    fun read(
        context: Context,
        userId: Int,
        fileName: String,
    ): BackupResult {
        val file = File(getUserFolder(context, userId), fileName)
        if (!file.exists()) throw NoSuchFileException(file)

        val json = file.readText()
        return Json.decodeFromString(BackupResult.serializer(), json)
    }
}
