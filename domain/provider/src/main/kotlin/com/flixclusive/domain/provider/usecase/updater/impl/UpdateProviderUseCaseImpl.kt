package com.flixclusive.domain.provider.usecase.updater.impl

import android.content.Context
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.core.datastore.model.user.ProviderFromPreferences
import com.flixclusive.core.datastore.model.user.ProviderPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.core.util.log.infoLog
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.domain.provider.R
import com.flixclusive.domain.provider.usecase.get.GetProviderFromRemoteUseCase
import com.flixclusive.domain.provider.usecase.manage.LoadProviderResult
import com.flixclusive.domain.provider.usecase.manage.LoadProviderUseCase
import com.flixclusive.domain.provider.usecase.manage.UnloadProviderUseCase
import com.flixclusive.domain.provider.usecase.updater.ProviderUpdateResult
import com.flixclusive.domain.provider.usecase.updater.UpdateProviderUseCase
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
import okhttp3.OkHttpClient
import java.io.File
import javax.inject.Inject

// TODO: Remove the usecase dependencies from the implementation.
//       It's a pain in the ass to test this class
internal class UpdateProviderUseCaseImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val dataStoreManager: DataStoreManager,
        private val userSessionDataStore: UserSessionDataStore,
        private val providerRepository: ProviderRepository,
        private val loadProviderUseCase: LoadProviderUseCase,
        private val unloadProviderUseCase: UnloadProviderUseCase,
        private val getProviderFromRemoteUseCase: GetProviderFromRemoteUseCase,
        private val client: OkHttpClient,
        private val appDispatchers: AppDispatchers,
    ) : UpdateProviderUseCase {
        private suspend fun getProviderPrefs() =
            dataStoreManager.getUserPrefs(UserPreferences.PROVIDER_PREFS_KEY, ProviderPreferences::class).first()

        @Throws(Throwable::class)
        override suspend fun invoke(provider: ProviderMetadata) {
            if (providerRepository.getProvider(provider.id) == null) {
                error(context.getString(R.string.provider_not_found, provider.name, provider.id))
            }

            val repository = provider.repositoryUrl.toValidRepositoryLink()
            val updatedMetadata = getProviderFromRemoteUseCase(
                repository = repository,
                id = provider.id,
            ).getOrThrow()

            val oldPreference = getOldPreferenceItem(provider.id)
            createBackup(oldPreference)

            val newPreference = getNewPreferenceItem(
                oldPreference = oldPreference,
                newMetadata = updatedMetadata,
            )

            try {
                withContext(appDispatchers.io) {
                    client.downloadProvider(
                        saveTo = File(newPreference.filePath),
                        buildUrl = updatedMetadata.buildUrl,
                    )
                }
            } catch (e: Throwable) {
                throw DownloadException(e)
            }

            try {
                unloadProviderUseCase(
                    metadata = provider,
                    unloadFromPrefs = false,
                )
            } catch (e: Throwable) {
                throw UnloadException(e)
            }

            providerRepository.addToPreferences(preferenceItem = newPreference)
            loadProviderUseCase(
                metadata = updatedMetadata,
                filePath = newPreference.filePath,
            ).onEach {
                // If the provider failed to load but it was
                // previously loaded, just log the exception
                if (it is LoadProviderResult.Failure &&
                    providerRepository.getProvider(provider.id) != null
                ) {
                    // If the provider is loaded, just log the exception and continue
                    infoLog("Provider ${provider.name} updated but failed to load with exception: ${it.error}")
                    return@onEach
                }

                // If the provider failed to load and it wasn't previously
                // loaded, restore the backup and throw an exception
                if (it is LoadProviderResult.Failure) {
                    restoreBackup(oldPreference)
                    providerRepository.addToPreferences(oldPreference)

                    loadProviderUseCase(
                        metadata = provider,
                        filePath = oldPreference.filePath,
                    ).collect()
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

        private suspend fun getOldPreferenceItem(id: String): ProviderFromPreferences {
            val providers = getProviderPrefs().providers
            val oldPreference = providers.find { it.id == id }

            if (oldPreference == null) {
                error(context.getString(R.string.provider_not_found_on_prefs, id))
            }

            return oldPreference
        }

        /**
         * Creates a new [ProviderFromPreferences] item with updated metadata and a new file path.
         *
         * This is necessary because the new metadata might not have the same name as the old one,
         *
         * @param oldPreference The old provider preference item.
         * @param newMetadata The new provider metadata.
         *
         * @return A new [ProviderFromPreferences] item with updated information.
         * */
        private suspend fun getNewPreferenceItem(
            oldPreference: ProviderFromPreferences,
            newMetadata: ProviderMetadata,
        ): ProviderFromPreferences {
            val userId = userSessionDataStore.currentUserId.filterNotNull().first()

            val file = context.createFileForProvider(
                userId = userId,
                provider = newMetadata,
            )

            return oldPreference.copy(
                name = newMetadata.name,
                filePath = file.absolutePath,
            )
        }

        /**
         * Creates a backup of all files in the same directory as the provider file.
         *
         * The backup files will have the same name as the original file with a .bak extension
         * appended to it.
         *
         * @param preference The provider preference item containing the file path.
         * */
        private suspend fun createBackup(preference: ProviderFromPreferences) {
            val directory = File(preference.filePath).parentFile ?: return

            withContext(appDispatchers.io) {
                directory.listFiles()?.forEach {
                    val backup = File(directory, "${it.name}.bak")
                    it.copyTo(backup, true)
                }
            }
        }

        /**
         * Restores the backup of all files in the same directory as the provider file.
         *
         * The backup files are expected to have a .bak extension appended to the original file name.
         * */
        private suspend fun restoreBackup(backup: ProviderFromPreferences) {
            val directory = File(backup.filePath).parentFile ?: return

            withContext(appDispatchers.io) {
                directory.listFiles()?.forEach {
                    if (!it.name.endsWith(".bak")) {
                        it.delete()
                        return@forEach
                    }

                    val original = File(directory, it.name.removeSuffix(".bak"))
                    it.copyTo(original, true)
                    it.delete()
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
