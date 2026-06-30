package com.flixclusive.domain.provider.usecase.manage.impl

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.provider.ProviderConstants
import com.flixclusive.core.common.provider.ProviderFile
import com.flixclusive.core.common.provider.ProviderFile.getProvidersSettingsPath
import com.flixclusive.core.database.entity.provider.InstalledProvider
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.core.datastore.model.user.ProviderPreferences
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.core.util.log.infoLog
import com.flixclusive.core.util.log.warnLog
import com.flixclusive.core.util.network.json.fromJson
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.domain.provider.R
import com.flixclusive.domain.provider.usecase.manage.LoadProviderUseCase
import com.flixclusive.domain.provider.usecase.manage.ProviderResult
import com.flixclusive.domain.provider.util.DynamicResourceLoader
import com.flixclusive.domain.provider.util.DynamicResourceLoader.getResourcesFromProvider
import com.flixclusive.domain.provider.util.extensions.getFileFromPath
import com.flixclusive.model.provider.Language
import com.flixclusive.model.provider.ProviderManifest
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.model.provider.ProviderStatus
import com.flixclusive.model.provider.ProviderType
import com.flixclusive.model.provider.Repository.Companion.toValidRepositoryLink
import com.flixclusive.provider.ProviderPlugin
import dagger.hilt.android.qualifiers.ApplicationContext
import dalvik.system.PathClassLoader
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

private const val MANIFEST_FILE = "manifest.json"

internal class LoadProviderUseCaseImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val userSessionDataStore: UserSessionDataStore,
    private val providerRepository: ProviderRepository,
    private val appDispatchers: AppDispatchers,
) : LoadProviderUseCase {
    private val cacheLocalMetadataMap by lazy { HashMap<String, ProviderMetadata>() }

    private val dataStores by lazy { ConcurrentHashMap<String, DataStore<Preferences>>() }

    // TODO: Create a separate service for loading providers
    //       since `InitializeProvidersUseCase` also needs to load providers
    @Suppress("UNCHECKED_CAST")
    override fun invoke(installedProvider: InstalledProvider): Flow<ProviderResult> =
        flow {
            val userId = userSessionDataStore.currentUserId.filterNotNull().first()
            val metadata = providerRepository
                .getProvider(installedProvider.id, userId)
                ?.metadata
                ?: getMetadataFromFile(installedProvider)

            if (metadata == null) {
                emit(
                    ProviderResult.Failure(
                        provider = createMissingMetadata(installedProvider),
                        error = IllegalStateException(
                            context.getString(R.string.missing_metadata, installedProvider.id)
                        ),
                    ),
                )
                return@flow
            }

            val filePath = installedProvider.filePath
            val file = installedProvider.file

            try {
                if (!file.exists()) {
                    errorLog("Provider file does not exist: $filePath")
                    emit(
                        ProviderResult.Failure(
                            provider = metadata,
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

                withContext(appDispatchers.io) {
                    if (!file.setReadOnly(metadata = metadata)) {
                        warnLog("Failed to set dex as read-only for provider: ${metadata.name}.")
                    }
                }

                infoLog("Loading provider: ${metadata.name} [${file.name}]")

                val loader = PathClassLoader(file.absolutePath, context.classLoader)
                val manifest = withContext<ProviderManifest>(appDispatchers.io) {
                    loader.getFileFromPath(MANIFEST_FILE)
                }.let {
                    if (!metadata.id.endsWith(ProviderPreferences.DEBUG_SUFFIX)) {
                        return@let it
                    }

                    it.copy(
                        id = "${it.id}${ProviderPreferences.DEBUG_SUFFIX}",
                        name = "${it.name}${ProviderPreferences.DEBUG_SUFFIX}"
                    )
                }

                val providerClass: Class<out ProviderPlugin?> =
                    loader.loadClass(manifest.providerClassName) as Class<out ProviderPlugin>
                val provider = providerClass.getDeclaredConstructor().newInstance() as ProviderPlugin

                val providerPrefs = dataStores.getOrPut(installedProvider.id) {
                    PreferenceDataStoreFactory.create {
                        val settingsDirPath = createSettingsDirPath(
                            userId = installedProvider.ownerId,
                            repositoryUrl = installedProvider.repositoryUrl,
                            isDebugProvider = installedProvider.isDebug,
                        )

                        File("$settingsDirPath/${manifest.id}.preferences_pb").apply {
                            parentFile?.mkdirs()
                        }
                    }
                }

                provider.manifest = manifest
                provider.settings = providerPrefs

                if (manifest.requiresResources) {
                    withContext(appDispatchers.io) {
                        provider.resources = context.getResourcesFromProvider(inputFile = file)

                        if (DynamicResourceLoader.needsCleanUp) {
                            DynamicResourceLoader.cleanupArtifacts(file)
                        }
                    }
                }

                providerRepository.load(
                    classLoader = loader,
                    provider = provider,
                    metadata = metadata,
                )

                emit(ProviderResult.Success(provider = metadata))
            } catch (e: Throwable) {
                emit(
                    ProviderResult.Failure(
                        provider = metadata,
                        error = e,
                    ),
                )
                errorLog("${metadata.name} crashed with an error!")
                errorLog(e)
            }
        }

    /**
     * On Android 14+, files created/downloaded by ADB or other external means
     * may be owned by external UIDs, causing `setReadOnly` to fail due to
     * permission issues.
     *
     * To ensure the provider can still be loaded, we fall back to creating an
     * app-owned copy of the file which we can set as read-only without restrictions.
     * */
    private fun File.setReadOnly(metadata: ProviderMetadata): Boolean {
        if (setReadOnly()) {
            return true
        }

        warnLog("Failed to set dex as read-only for provider: ${metadata.name}. Replacing with app-owned copy...")
        val tmpFile = File(parentFile, "$nameWithoutExtension.tmp")
        try {
            copyTo(target = tmpFile, overwrite = true)
            delete()
            tmpFile.renameTo(this)

            if (!setReadOnly()) {
                return false
            }

            infoLog("Replaced with app-owned copy for provider: ${metadata.name}")
            return true
        } catch (e: Throwable) {
            tmpFile.delete()
            errorLog("Failed to replace with app-owned copy for provider: ${metadata.name}")
            errorLog(e)
            return false
        }
    }

    /**
     * Retrieves the metadata from the `updater.json` file for the given provider ID.
     *
     * All online metadata are stored in the `updater.json` file
     * */
    private fun getMetadataFromFile(provider: InstalledProvider): ProviderMetadata? {
        if (cacheLocalMetadataMap.containsKey(provider.id)) {
            return cacheLocalMetadataMap[provider.id]
        }

        val updaterFilePath = provider.file.parent?.plus("/${ProviderConstants.UPDATER_JSON_FILE}")

        if (updaterFilePath == null) {
            errorLog("Provider's file path must not be null!")
            return null
        }

        val updaterFile = File(updaterFilePath)

        if (!updaterFile.exists()) {
            errorLog("Provider's updater.json could not be found!")
            return null
        }

        val updaterJsonList = fromJson<List<ProviderMetadata>>(updaterFile.reader())
        updaterJsonList.forEach { metadata ->
            cacheLocalMetadataMap[metadata.id] = metadata
        }

        return cacheLocalMetadataMap[provider.id]
    }

    private fun createMissingMetadata(provider: InstalledProvider): ProviderMetadata =
        ProviderMetadata(
            id = provider.id,
            name = provider.file.nameWithoutExtension,
            repositoryUrl = "",
            buildUrl = "",
            authors = emptyList(),
            versionName = "Unknown",
            versionCode = -1,
            description = "No description available.",
            iconUrl = "",
            language = Language("Unknown"),
            providerType = ProviderType("Unknown"),
            status = ProviderStatus.Down,
        )

    private fun createSettingsDirPath(
        userId: String,
        repositoryUrl: String,
        isDebugProvider: Boolean,
    ): String {
        val repository = repositoryUrl.toValidRepositoryLink()
        val childDirectoryName = "${repository.owner}-${repository.name}"

        return if (isDebugProvider) {
            ProviderFile.getDebugProvidersSettingsPath() + "/$childDirectoryName"
        } else {
            context.getProvidersSettingsPath(userId) + "/$childDirectoryName"
        }
    }
}
