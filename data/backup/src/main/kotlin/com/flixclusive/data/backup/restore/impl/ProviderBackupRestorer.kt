package com.flixclusive.data.backup.restore.impl

import android.content.Context
import com.flixclusive.core.common.provider.ProviderConstants
import com.flixclusive.core.common.provider.ProviderFile.getDebugProvidersPath
import com.flixclusive.core.common.provider.ProviderFile.getProvidersPath
import com.flixclusive.core.database.dao.provider.InstalledProviderDao
import com.flixclusive.core.database.entity.provider.InstalledProvider
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.core.datastore.model.user.ProviderPreferences
import com.flixclusive.core.util.network.json.fromJson
import com.flixclusive.data.backup.model.BackupProvider
import com.flixclusive.data.backup.restore.BackupRestorer
import com.flixclusive.model.provider.ProviderMetadata
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import java.io.File
import java.util.Date
import javax.inject.Inject

internal class ProviderBackupRestorer @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val installedProviderDao: InstalledProviderDao,
    private val userSessionDataStore: UserSessionDataStore,
) : BackupRestorer<BackupProvider> {
    override suspend fun invoke(items: List<BackupProvider>): Result<Unit> {
        return runCatching {
            val ownerId = userSessionDataStore.currentUserId.filterNotNull().first()

            installedProviderDao.deleteAll(ownerId)

            val providerFilesIndex = buildProviderFileIndex(ownerId)

            items.forEach { provider ->
                val fileInfo = providerFilesIndex[provider.id] ?: return@forEach

                installedProviderDao.insert(
                    InstalledProvider(
                        ownerId = ownerId,
                        id = provider.id,
                        repositoryUrl = provider.repositoryUrl,
                        filePath = fileInfo.path,
                        sortOrder = provider.sortOrder,
                        isEnabled = provider.isEnabled,
                        isDebug = fileInfo.isDebug,
                        createdAt = Date(provider.createdAt),
                        updatedAt = Date(provider.updatedAt),
                    )
                )
            }
        }
    }

    private fun buildProviderFileIndex(ownerId: Int): Map<String, ProviderFileInfo> {
        val index = HashMap<String, ProviderFileInfo>()

        indexProvidersInRoot(
            index = index,
            root = File(context.getProvidersPath(ownerId)),
            isDebug = false,
            includeDebugSuffixMapping = false,
        )

        val debugPath = context.getDebugProvidersPath()
        indexProvidersInRoot(
            index = index,
            root = File(debugPath),
            isDebug = true,
            includeDebugSuffixMapping = true,
        )

        return index
    }

    private fun indexProvidersInRoot(
        index: MutableMap<String, ProviderFileInfo>,
        root: File,
        isDebug: Boolean,
        includeDebugSuffixMapping: Boolean,
    ) {
        if (!root.exists() || !root.isDirectory) return

        root.listFiles()?.forEach { repositoryDir ->
            if (!repositoryDir.isDirectory) return@forEach

            val updaterFile = File(repositoryDir, ProviderConstants.UPDATER_JSON_FILE)
            if (!updaterFile.exists()) return@forEach

            val metadataList = runCatching {
                updaterFile.reader().use { reader ->
                    fromJson<List<ProviderMetadata>>(reader)
                }
            }.getOrNull() ?: return@forEach

            metadataList.forEach { metadata ->
                val filename = metadata.buildUrl.substringAfterLast("/")
                val providerFile = File(repositoryDir, filename)

                if (!providerFile.exists()) return@forEach

                val fileInfo = ProviderFileInfo(
                    path = providerFile.absolutePath,
                    isDebug = isDebug,
                )

                index.putIfAbsent(metadata.id, fileInfo)
                if (includeDebugSuffixMapping) {
                    index.putIfAbsent(metadata.id + ProviderPreferences.DEBUG_PREFIX, fileInfo)
                }
            }
        }
    }

    private data class ProviderFileInfo(
        val path: String,
        val isDebug: Boolean,
    )
}
