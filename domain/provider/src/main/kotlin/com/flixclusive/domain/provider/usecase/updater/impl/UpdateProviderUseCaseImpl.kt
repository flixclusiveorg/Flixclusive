package com.flixclusive.domain.provider.usecase.updater.impl

import android.content.Context
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.database.entity.provider.InstalledProvider
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.core.util.log.infoLog
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.domain.downloads.usecase.DownloadFileUseCase
import com.flixclusive.domain.provider.R
import com.flixclusive.domain.provider.usecase.get.GetProviderFromRemoteUseCase
import com.flixclusive.domain.provider.usecase.manage.LoadProviderUseCase
import com.flixclusive.domain.provider.usecase.manage.ProviderResult
import com.flixclusive.domain.provider.usecase.manage.UnloadProviderUseCase
import com.flixclusive.domain.provider.usecase.updater.ProviderUpdateResult
import com.flixclusive.domain.provider.usecase.updater.UpdateProviderUseCase
import com.flixclusive.domain.provider.util.Constants
import com.flixclusive.domain.provider.util.extensions.createFileForProvider
import com.flixclusive.domain.provider.util.extensions.downloadProvider
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.model.provider.Repository.Companion.toValidRepositoryLink
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

// TODO: Remove the usecase dependencies from the implementation.
//       It's a pain in the ass to test this class
internal class UpdateProviderUseCaseImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val userSessionDataStore: UserSessionDataStore,
    private val providerRepository: ProviderRepository,
    private val loadProviderUseCase: LoadProviderUseCase,
    private val unloadProviderUseCase: UnloadProviderUseCase,
    private val downloadFileUseCase: DownloadFileUseCase,
    private val getProviderFromRemoteUseCase: GetProviderFromRemoteUseCase,
    private val appDispatchers: AppDispatchers,
) : UpdateProviderUseCase {

    @Throws(Throwable::class)
    override suspend fun invoke(provider: ProviderMetadata) {
        if (providerRepository.getPlugin(provider.id) == null) {
            error(context.getString(R.string.provider_not_found, provider.name, provider.id))
        }

        val repository = provider.repositoryUrl.toValidRepositoryLink()
        val updatedMetadata = getProviderFromRemoteUseCase(
            repository = repository,
            id = provider.id,
        ).getOrThrow()

        val old = getOldProviderConfig(provider.id)
        createBackup(old)

        val new = getNewPreferenceItem(
            oldPreference = old,
            newMetadata = updatedMetadata,
        )


        val oldFile = File(old.filePath)
        val newFile = File(new.filePath)

        withContext(appDispatchers.io) {
            oldFile.delete()
            downloadFileUseCase.downloadProvider(
                metadata = updatedMetadata,
                file = newFile,
            )
        }

        if (!newFile.exists()) {
            withContext(appDispatchers.io) { restoreBackup(old) }
            throw DownloadException(
                Throwable(
                    message = context.getString(
                        R.string.failed_to_download_provider,
                        provider.name,
                        provider.id,
                    )
                )
            )
        }

        try {
            unloadProviderUseCase(provider = old)
        } catch (e: Throwable) {
            throw UnloadException(e)
        }

        providerRepository.install(new)
        loadProviderUseCase(installedProvider = new).onEach {
            // If the provider failed to load, but it was
            // previously loaded, just log the exception
            if (it is ProviderResult.Failure && providerRepository.getPlugin(provider.id) != null) {
                // If the provider is loaded, just log the exception and continue
                infoLog("Provider ${provider.name} updated but failed to load with exception: ${it.error}")
                return@onEach
            }

            // If the provider failed to load, and it wasn't previously
            // loaded, restore the backup and throw an exception
            if (it is ProviderResult.Failure) {
                restoreBackup(old)
                providerRepository.install(old)
                loadProviderUseCase(installedProvider = old).collect()
                throw LoadException(it.error)
            }
        }.collect()
    }

    override suspend fun invoke(providers: List<ProviderMetadata>): ProviderUpdateResult {
        val updatedProviders = mutableListOf<ProviderMetadata>()
        val failedToUpdateProviders = mutableListOf<Pair<ProviderMetadata, Throwable>>()

        for (provider in providers) {
            val error = try {
                invoke(provider)
                updatedProviders.add(provider)
                null
            } catch (e: DownloadException) {
                Throwable(
                    message = context.getString(
                        R.string.failed_to_download_provider,
                        provider.name,
                        provider.id,
                    ),
                    cause = e.cause!!,
                )
            } catch (e: UnloadException) {
                Throwable(
                    cause = e.cause!!,
                    message = context.getString(
                        R.string.unload_exception_message,
                        provider.name,
                        provider.id,
                        e.cause?.localizedMessage ?: "Unknown cause",
                    ),
                )
            } catch (e: LoadException) {
                Throwable(
                    message = context.getString(
                        R.string.failed_to_load_provider,
                        provider.name,
                        provider.id,
                    ),
                    cause = e.cause!!,
                )
            } catch (e: Throwable) {
                e
            }

            if (error != null) {
                failedToUpdateProviders.add(provider to error)
            }
        }

        return ProviderUpdateResult(
            success = updatedProviders,
            failed = failedToUpdateProviders,
        )
    }

    private suspend fun getOldProviderConfig(id: String): InstalledProvider {
        val userId = userSessionDataStore.currentUserId.filterNotNull().first()

        val old = providerRepository.getConfig(id, userId) ?: error(
            context.getString(
                R.string.provider_not_even_installed,
                id
            )
        )

        return old
    }

    private suspend fun getNewPreferenceItem(
        oldPreference: InstalledProvider,
        newMetadata: ProviderMetadata,
    ): InstalledProvider {
        val userId = userSessionDataStore.currentUserId.filterNotNull().first()

        val file = context.createFileForProvider(
            userId = userId,
            provider = newMetadata,
        )

        return oldPreference.copy(
            filePath = file.absolutePath,
        )
    }

    private suspend fun createBackup(provider: InstalledProvider) {
        val file = File(provider.filePath)
        val directory = file.parentFile ?: return
        val updaterJsonFile = File(directory, Constants.UPDATER_FILE)

        withContext(appDispatchers.io) {
            // Backup provider file
            file.copyTo(
                target = File(directory, "${file.name}.bak"),
                overwrite = true,
            )

            // Backup updater.json
            updaterJsonFile.copyTo(
                target = File(directory, "${updaterJsonFile.name}.bak"),
                overwrite = true,
            )
        }
    }

    private suspend fun restoreBackup(backup: InstalledProvider) {
        val file = File(backup.filePath)
        val directory = file.parentFile ?: return
        val updaterJsonFile = File(directory, Constants.UPDATER_FILE)

        val backupFile = File(directory, "${file.name}.bak")
        val backupUpdaterJsonFile = File(directory, "${updaterJsonFile.name}.bak")

        withContext(appDispatchers.io) {
            if (backupFile.exists()) {
                backupFile.copyTo(target = file, overwrite = true)
                backupFile.delete()
            }

            if (backupUpdaterJsonFile.exists()) {
                backupUpdaterJsonFile.copyTo(target = updaterJsonFile, overwrite = true)
                backupUpdaterJsonFile.delete()
            }
        }
    }
}

internal class DownloadException(
    cause: Throwable,
) : Throwable(cause)

internal class LoadException(
    cause: Throwable,
) : Throwable(cause)

internal class UnloadException(
    cause: Throwable,
) : Throwable(cause)
