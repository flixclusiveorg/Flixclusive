package com.flixclusive.domain.provider.usecase.manage.impl

import android.content.Context
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.file.isEmpty
import com.flixclusive.core.common.provider.ProviderConstants
import com.flixclusive.core.common.provider.ProviderFile.getProviderSettingsFileDirPath
import com.flixclusive.core.database.entity.provider.InstalledProvider
import com.flixclusive.core.util.log.infoLog
import com.flixclusive.core.util.log.warnLog
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.domain.provider.R
import com.flixclusive.domain.provider.usecase.manage.UnloadProviderUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

internal class UnloadProviderUseCaseImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val providerRepository: ProviderRepository,
    private val appDispatchers: AppDispatchers,
) : UnloadProviderUseCase {
    override suspend operator fun invoke(
        provider: InstalledProvider,
        uninstall: Boolean,
    ) {
        val providerWrapper = providerRepository.getProvider(id = provider.id, ownerId = provider.ownerId)
            ?: error(context.getString(R.string.provider_not_even_installed, provider.id))

        val file = provider.file
        if (!file.exists()) {
            warnLog("Provider file not found for provider: ${providerWrapper.name} at path: ${file.absolutePath}")
        }

        infoLog("Unloading provider: ${providerWrapper.name}")
        try {
            if (uninstall) {
                providerRepository.uninstall(provider = provider)
            } else {
                providerWrapper.plugin?.onUnload(context)
            }
        } catch (e: Throwable) {
            throw Throwable(
                cause = e,
                message = context.getString(
                    R.string.unload_exception_message,
                    providerWrapper.name,
                    providerWrapper.id,
                    e.localizedMessage,
                ),
            )
        }

        withContext(appDispatchers.io) {
            deleteProviderRelatedFiles(
                provider = provider,
                file = file
            )
        }
    }

    private fun deleteProviderRelatedFiles(provider: InstalledProvider, file: File) {
        file.delete()

        // Delete updater.json file if it's the only thing remaining on that directory
        val parentDirectory = file.parentFile!!
        if (parentDirectory.isDirectory && parentDirectory.listFiles()?.size == 1) {
            val lastRemainingFile = parentDirectory.listFiles()!![0]

            if (lastRemainingFile.name.equals(ProviderConstants.UPDATER_JSON_FILE, true)) {
                parentDirectory.deleteRecursively()
            }
        }

        // Delete provider settings
        val settingsDirPath = context.getProviderSettingsFileDirPath(
            userId = provider.ownerId,
            isDebugProvider = provider.isDebug,
            repositoryUrl = provider.repositoryUrl
        )

        val settingsDirectory = File(settingsDirPath)
        if (!settingsDirectory.exists()) return

        settingsDirectory
            .listFiles {
                // Matching with "." since we might delete debug provider prefs
                it.name.contains("${provider.id}.", true)
            }?.forEach { it.delete() }

        if (settingsDirectory.isEmpty()) {
            settingsDirectory.delete()
        }
    }
}
