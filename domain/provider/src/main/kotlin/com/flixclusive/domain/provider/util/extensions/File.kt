package com.flixclusive.domain.provider.util.extensions

import android.content.Context
import com.flixclusive.core.common.provider.ProviderFile.getProvidersDirPath
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.model.provider.Repository.Companion.toValidRepositoryLink
import java.io.File

internal fun Context.createFileForProvider(
    provider: ProviderMetadata,
    userId: String,
): File {
    val prefix = getProvidersDirPath(userId)
    val repository = provider.repositoryUrl.toValidRepositoryLink()
    val filename = provider.buildUrl.substringAfterLast("/")
    val folderName = "${repository.owner}-${repository.name}"

    return File("$prefix/$folderName/$filename")
}
