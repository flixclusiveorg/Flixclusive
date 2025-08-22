package com.flixclusive.domain.provider.usecase.manage.impl

import android.content.Context
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.PROVIDERS_SETTINGS_FOLDER_NAME
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.core.datastore.model.user.ProviderFromPreferences
import com.flixclusive.core.datastore.model.user.ProviderPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.core.util.log.infoLog
import com.flixclusive.core.util.log.warnLog
import com.flixclusive.data.provider.repository.ProviderApiRepository
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.domain.provider.R
import com.flixclusive.domain.provider.usecase.manage.LoadProviderResult
import com.flixclusive.domain.provider.usecase.manage.LoadProviderUseCase
import com.flixclusive.domain.provider.util.Constants
import com.flixclusive.domain.provider.util.DynamicResourceLoader
import com.flixclusive.domain.provider.util.ProviderMigrator
import com.flixclusive.domain.provider.util.ProviderMigrator.canMigrateSettingsFile
import com.flixclusive.domain.provider.util.extensions.createFileForProvider
import com.flixclusive.domain.provider.util.extensions.downloadProvider
import com.flixclusive.domain.provider.util.extensions.getFileFromPath
import com.flixclusive.domain.provider.util.extensions.getProviderInstance
import com.flixclusive.model.provider.ProviderManifest
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.model.provider.Repository.Companion.toValidRepositoryLink
import dagger.hilt.android.qualifiers.ApplicationContext
import dalvik.system.PathClassLoader
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.File
import java.io.FileNotFoundException
import javax.inject.Inject

private const val MANIFEST_FILE = "manifest.json"

internal class LoadProviderUseCaseImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val client: OkHttpClient,
        private val userSessionDataStore: UserSessionDataStore,
        private val dataStoreManager: DataStoreManager,
        private val providerRepository: ProviderRepository,
        private val providerApiRepository: ProviderApiRepository,
        private val appDispatchers: AppDispatchers,
    ) : LoadProviderUseCase {
        private val dynamicResourceLoader by lazy { DynamicResourceLoader(context = context) }

        private suspend fun getProviderPrefs() =
            dataStoreManager
                .getUserPrefs(UserPreferences.PROVIDER_PREFS_KEY, ProviderPreferences::class)
                .first()

        override fun invoke(metadata: ProviderMetadata): Flow<LoadProviderResult> =
            channelFlow {
                val userId = userSessionDataStore.currentUserId.filterNotNull().first()
                val file = context.createFileForProvider(
                    provider = metadata,
                    userId = userId,
                )

                if (isProviderAlreadyLoaded(metadata)) {
                    send(
                        LoadProviderResult.Failure(
                            provider = metadata,
                            filePath = file.absolutePath,
                            error = IllegalStateException(
                                context.getString(R.string.provider_already_exists, metadata.name)
                            ),
                        ),
                    )
                    return@channelFlow
                }

                val success = withContext(appDispatchers.io) {
                    try {
                        client.downloadProvider(
                            saveTo = file,
                            buildUrl = metadata.buildUrl,
                        )
                    } catch (e: Throwable) {
                        errorLog("Failed to download provider: ${metadata.name} [${file.name}]")
                        errorLog(e)
                        send(
                            LoadProviderResult.Failure(
                                provider = metadata,
                                filePath = file.absolutePath,
                                error = e,
                            ),
                        )
                        return@withContext false
                    }

                    return@withContext true
                }

                if (!success) {
                    return@channelFlow
                }

                invoke(
                    filePath = file.absolutePath,
                    metadata = metadata,
                ).collect(::send)
            }

        // TODO: Create a separate service for loading providers
        //       since `InitializeProvidersUseCase` also needs to load providers
        override fun invoke(
            metadata: ProviderMetadata,
            filePath: String,
        ): Flow<LoadProviderResult> =
            flow<LoadProviderResult> {
                if (isProviderAlreadyLoaded(metadata)) {
                    emit(
                        LoadProviderResult.Failure(
                            provider = metadata,
                            filePath = filePath,
                            error = IllegalStateException(
                                context.getString(R.string.provider_already_exists, metadata.name)
                            ),
                        ),
                    )
                    return@flow
                }

                try {
                    val file = File(filePath)
                    val isFileExisting = withContext(appDispatchers.io) {
                        safeCall {
                            file.setReadOnly()
                        }

                        return@withContext file.exists()
                    }

                    if (!isFileExisting) {
                        errorLog("Provider file does not exist: $filePath")
                        emit(
                            LoadProviderResult.Failure(
                                provider = metadata,
                                filePath = filePath,
                                error = FileNotFoundException(
                                    context.getString(
                                        R.string.provider_file_not_found,
                                        filePath,
                                    ),
                                ),
                            ),
                        )
                        return@flow
                    }

                    infoLog("Loading provider: ${metadata.name} [${file.name}]")

                    val loader = PathClassLoader(filePath, context.classLoader)
                    val manifest: ProviderManifest = withContext(appDispatchers.io) {
                        loader.getFileFromPath(MANIFEST_FILE)
                    }
                    val settingsDirPath = createSettingsDirPath(
                        repositoryUrl = metadata.repositoryUrl,
                        isDebugProvider = metadata.id.endsWith(Constants.PROVIDER_DEBUG),
                    )

                    val preferenceItem = getPreferenceItemOrCreate(
                        id = metadata.id,
                        fileName = file.nameWithoutExtension,
                        filePath = filePath,
                    )

                    if (getProviderPrefs().canMigrateSettingsFile(metadata)) {
                        withContext(appDispatchers.io) {
                            ProviderMigrator.migrateForOldSettingsFile(
                                directory = settingsDirPath,
                                metadata = metadata,
                            )
                        }
                    }

                    val provider = loader.getProviderInstance(
                        id = metadata.id,
                        file = file,
                        manifest = manifest,
                        settingsDirPath = settingsDirPath,
                    )

                    if (manifest.requiresResources) {
                        withContext(appDispatchers.io) {
                            provider.resources = dynamicResourceLoader.load(inputFile = file)

                            if (dynamicResourceLoader.forceCleanUp) {
                                dynamicResourceLoader.cleanupArtifacts(file)
                            }
                        }
                    }

                    var isApiDisabled = preferenceItem.isDisabled
                    try {
                        if (!isApiDisabled) {
                            providerApiRepository.addApiFromProvider(
                                id = metadata.id,
                                provider = provider,
                            )
                        }

                        emit(LoadProviderResult.Success(provider = metadata))
                    } catch (e: Throwable) {
                        isApiDisabled = true

                        emit(
                            LoadProviderResult.Failure(
                                provider = metadata,
                                filePath = filePath,
                                error = e,
                            ),
                        )
                        errorLog(e)
                    } finally {
                        providerRepository.add(
                            classLoader = loader,
                            provider = provider,
                            metadata = metadata,
                            preferenceItem = preferenceItem.copy(isDisabled = isApiDisabled),
                        )
                    }
                } catch (e: Throwable) {
                    emit(
                        LoadProviderResult.Failure(
                            provider = metadata,
                            filePath = filePath,
                            error = e,
                        ),
                    )
                    errorLog("${metadata.name} crashed with an error!")
                    errorLog(e)
                }
            }

        private suspend fun createSettingsDirPath(
            repositoryUrl: String,
            isDebugProvider: Boolean,
        ): String {
            val userId = userSessionDataStore.currentUserId.filterNotNull().first()
            val parentDirectoryName = if (isDebugProvider) Constants.PROVIDER_DEBUG else "user-$userId"

            val repository = repositoryUrl.toValidRepositoryLink()
            val childDirectoryName = "${repository.owner}-${repository.name}"
            val finalPathPrefix = "$PROVIDERS_SETTINGS_FOLDER_NAME/$parentDirectoryName/$childDirectoryName"

            return "${context.getExternalFilesDir(null)}/$finalPathPrefix"
        }

        private suspend fun getPreferenceItemOrCreate(
            id: String,
            fileName: String,
            filePath: String,
        ): ProviderFromPreferences {
            var providerFromPreferences =
                getProviderPrefs()
                    .providers
                    .find { it.id == id }

            if (providerFromPreferences == null) {
                providerFromPreferences =
                    ProviderFromPreferences(
                        id = id,
                        name = fileName,
                        filePath = filePath,
                        isDisabled = false,
                    )
            }

            return providerFromPreferences
        }

        private fun isProviderAlreadyLoaded(
            metadata: ProviderMetadata,
        ): Boolean {
            if (providerRepository.getProvider(metadata.id) != null) {
                warnLog("Provider with name ${metadata.name} already exists")
                return true
            }

            return false
        }
    }
