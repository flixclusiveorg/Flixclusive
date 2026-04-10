package com.flixclusive.data.backup.validate

internal enum class BackupValidationMode {
    CREATE,
    RESTORE,
}

internal interface BackupValidator<T> {
    suspend operator fun invoke(
        backup: List<T>,
        mode: BackupValidationMode,
    ): Result<Set<String>>
}
