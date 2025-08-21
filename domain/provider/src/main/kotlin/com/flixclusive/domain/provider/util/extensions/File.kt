package com.flixclusive.domain.provider.util.extensions

import android.content.Context
import com.flixclusive.core.datastore.util.getProvidersPathPrefix
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.model.provider.Repository.Companion.toValidRepositoryLink
import java.io.File

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

private const val PROVIDER_FILE_SUFFIX = ".flx"

internal val File.isProviderFile: Boolean
    get() = name.endsWith(PROVIDER_FILE_SUFFIX, true)
