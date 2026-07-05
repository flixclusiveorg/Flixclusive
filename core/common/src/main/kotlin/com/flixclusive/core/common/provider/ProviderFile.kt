package com.flixclusive.core.common.provider

import android.content.Context
import android.os.Environment
import com.flixclusive.core.common.provider.ProviderConstants.PROVIDERS_FOLDER_NAME
import com.flixclusive.core.common.provider.ProviderConstants.PROVIDERS_SETTINGS_FOLDER_NAME
import com.flixclusive.core.common.provider.ProviderConstants.PROVIDER_DEBUG
import com.flixclusive.model.provider.Repository.Companion.toValidRepositoryLink

object ProviderFile {
    /**
     * Returns the path prefix for the providers folder for a specific user.
     *
     * @param userId The ID of the user.
     * @return The path prefix for the providers' folder.
     */
    fun Context.getProvidersDirPath(userId: String): String =
        getExternalFilesDir(null)?.absolutePath + "/$PROVIDERS_FOLDER_NAME/user-$userId"

    /**
     * Returns the path prefix for the providers settings folder for a specific user.
     *
     * @param userId The ID of the user.
     * @return The path prefix for the providers settings folder.
     */
    fun Context.getProvidersSettingsRootDirPath(userId: String): String =
        getExternalFilesDir(null)?.absolutePath + "/$PROVIDERS_SETTINGS_FOLDER_NAME/user-$userId"

    fun getDebugProvidersDirPath(): String =
        "${Environment.getExternalStorageDirectory().absolutePath}/Flixclusive/$PROVIDERS_FOLDER_NAME/$PROVIDER_DEBUG"

    fun getDebugProvidersSettingsDirPath(): String =
        "${Environment.getExternalStorageDirectory().absolutePath}/Flixclusive/$PROVIDERS_SETTINGS_FOLDER_NAME/$PROVIDER_DEBUG"

    fun Context.getProviderSettingsFileDirPath(
        userId: String,
        repositoryUrl: String,
        isDebugProvider: Boolean
    ): String {
        val repository = repositoryUrl.toValidRepositoryLink()
        val childDirectoryName = "${repository.owner}-${repository.name}"

        return if (isDebugProvider) {
            getDebugProvidersSettingsDirPath() + "/$childDirectoryName"
        } else {
            getProvidersSettingsRootDirPath(userId) + "/$childDirectoryName"
        }
    }
}
