package com.flixclusive.core.common.provider

import android.content.Context
import com.flixclusive.core.common.provider.ProviderConstants.PROVIDERS_FOLDER_NAME
import com.flixclusive.core.common.provider.ProviderConstants.PROVIDERS_SETTINGS_FOLDER_NAME
import com.flixclusive.core.common.provider.ProviderConstants.PROVIDER_DEBUG

object ProviderFile {

    /**
     * Returns the path prefix for the providers folder for a specific user.
     *
     * @param userId The ID of the user.
     * @return The path prefix for the providers' folder.
     */
    fun Context.getProvidersPath(userId: Int): String =
        getExternalFilesDir(null)?.absolutePath + "/$PROVIDERS_FOLDER_NAME/user-$userId"

    /**
     * Returns the path prefix for the providers settings folder for a specific user.
     *
     * @param userId The ID of the user.
     * @return The path prefix for the providers settings folder.
     */
    fun Context.getProvidersSettingsPath(userId: Int): String =
        getExternalFilesDir(null)?.absolutePath + "/$PROVIDERS_SETTINGS_FOLDER_NAME/user-$userId"

    fun Context.getDebugProvidersPath(): String =
        getExternalFilesDir(null)?.absolutePath + "/$PROVIDERS_FOLDER_NAME/$PROVIDER_DEBUG"
}
