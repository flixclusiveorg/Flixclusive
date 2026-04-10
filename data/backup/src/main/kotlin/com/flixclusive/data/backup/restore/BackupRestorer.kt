package com.flixclusive.data.backup.restore

internal interface BackupRestorer<T> {
    suspend operator fun invoke(items: List<T>): Result<Unit>
}
