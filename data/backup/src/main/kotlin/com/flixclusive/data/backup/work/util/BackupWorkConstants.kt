package com.flixclusive.data.backup.work.util

internal object BackupWorkConstants {
    const val INPUT_USER_ID = "userId"
    const val INPUT_URI = "uri"

    const val INPUT_INCLUDE_LIBRARY = "includeLibrary"
    const val INPUT_INCLUDE_WATCH_PROGRESS = "includeWatchProgress"
    const val INPUT_INCLUDE_SEARCH_HISTORY = "includeSearchHistory"
    const val INPUT_INCLUDE_PREFERENCES = "includePreferences"
    const val INPUT_INCLUDE_PROVIDERS = "includeProviders"
    const val INPUT_INCLUDE_REPOSITORIES = "includeRepositories"

    const val OUTPUT_ERROR_MESSAGE = "errorMessage"

    const val UNIQUE_AUTO_BACKUP_CREATE_PREFIX = "auto_backup_create_user_"
    const val UNIQUE_BACKUP_CREATE_PREFIX = "backup_create_user_"
    const val UNIQUE_BACKUP_RESTORE_PREFIX = "backup_restore_user_"

    const val TAG_AUTO_BACKUP_CREATE = "auto_backup_create"
    const val TAG_BACKUP_CREATE = "backup_create"
    const val TAG_BACKUP_RESTORE = "backup_restore"

    const val TAG_BACKUP_CREATE_USER_PREFIX = "backup_create_user_"
    const val TAG_BACKUP_RESTORE_USER_PREFIX = "backup_restore_user_"

    const val LAST_CREATE_RESULT_FILE_NAME = "result-create.json"
    const val LAST_RESTORE_RESULT_FILE_NAME = "restore-result.json"

    const val BACKUP_FILE_PREFIX = "auto_backup-"
}
