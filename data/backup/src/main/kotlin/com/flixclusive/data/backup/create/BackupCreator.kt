package com.flixclusive.data.backup.create

internal interface BackupCreator<T> {
    suspend operator fun invoke(): Result<List<T>>
}
