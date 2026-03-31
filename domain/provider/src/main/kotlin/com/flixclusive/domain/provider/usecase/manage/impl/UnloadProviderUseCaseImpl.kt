package com.flixclusive.domain.provider.usecase.manage.impl

import android.content.Context
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.database.entity.provider.InstalledProvider
import com.flixclusive.core.datastore.util.rmrf
import com.flixclusive.core.util.log.infoLog
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.domain.provider.R
import com.flixclusive.domain.provider.usecase.manage.UnloadProviderUseCase
import com.flixclusive.domain.provider.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

internal class UnloadProviderUseCaseImpl
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val providerRepository: ProviderRepository,
        private val appDispatchers: AppDispatchers,
    ) : UnloadProviderUseCase {
        override suspend operator fun invoke(
            provider: InstalledProvider,
            uninstall: Boolean,
        ) {
            val metadata = providerRepository.getMetadata(provider.id)
                ?: error(context.getString(R.string.provider_not_even_installed, provider.id))

            val file = provider.file
            if (!file.exists()) {
                error(context.getString(R.string.provider_not_found, metadata.name, metadata.id))
            }

            infoLog("Unloading provider: ${metadata.name}")
            try {
                if (uninstall) {
                    providerRepository.uninstall(provider = provider)
                } else {
                    providerRepository.unload(id = metadata.id)
                }
            } catch (e: Throwable) {
                throw Throwable(
                    cause = e,
                    message = context.getString(
                        R.string.unload_exception_message,
                        metadata.name,
                        metadata.id,
                        e.localizedMessage,
                    ),
                )
            }

            withContext(appDispatchers.io) {
                deleteProviderRelatedFiles(file = file)
            }
        }

        private fun deleteProviderRelatedFiles(file: File) {
            file.delete()

            // Delete updater.json file if its the only thing remaining on that directory
            val parentDirectory = file.parentFile!!
            if (parentDirectory.isDirectory && parentDirectory.listFiles()?.size == 1) {
                val lastRemainingFile = parentDirectory.listFiles()!![0]

                if (lastRemainingFile.name.equals(Constants.UPDATER_FILE, true)) {
                    rmrf(parentDirectory)
                }
            }
        }
    }
