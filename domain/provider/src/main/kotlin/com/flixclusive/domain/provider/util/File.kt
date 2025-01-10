package com.flixclusive.domain.provider.util

import android.content.Context
import com.flixclusive.domain.provider.PROVIDERS_FOLDER_NAME
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.model.provider.Repository.Companion.toValidRepositoryLink
import java.io.File

/**
 *
 * Deletes recursively
 * */
internal fun rmrf(file: File) {
    if (file.isDirectory) {
        val files = file.listFiles() ?: emptyArray()
        for (subFiles in files) rmrf(subFiles)
    }

    file.delete()
}

internal fun Context.getExternalDirPath(): String? {
    val externalDir = getExternalFilesDir(null)
    val externalDirPath = externalDir?.absolutePath

    return externalDirPath
}

private fun Context.getProvidersPathPrefix(userId: Int): String =
    getExternalDirPath() + "/${PROVIDERS_FOLDER_NAME}/user-$userId"

internal fun Context.createFileForProvider(
    provider: ProviderMetadata,
    userId: Int,
): File {
    val prefix = getProvidersPathPrefix(userId)
    val repository = provider.repositoryUrl.toValidRepositoryLink()
    val filename = provider.buildUrl.substringAfterLast("/")
    val folderName = "${repository.owner}-${repository.name}"

    return File("$prefix/$folderName/$filename")
}

private const val OAT_FILENAME = "oat"
private const val CLASSES_DEX_FILENAME = "classes.dex"
private const val JSON_SUFFIX = ".json"
private const val PROVIDER_FILE_SUFFIX = ".flx"

internal val File.isNotOat: Boolean
    get() = name.equals(OAT_FILENAME, true)

internal val File.isClassesDex: Boolean
    get() = name.equals(CLASSES_DEX_FILENAME, true)

internal val File.isJson: Boolean
    get() = name.endsWith(JSON_SUFFIX, true)

internal val File.isProviderFile: Boolean
    get() = name.endsWith(PROVIDER_FILE_SUFFIX, true)
