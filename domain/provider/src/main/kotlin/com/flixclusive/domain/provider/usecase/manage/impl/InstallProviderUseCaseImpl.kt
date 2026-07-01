package com.flixclusive.domain.provider.usecase.manage.impl

import android.content.Context
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.exception.ExceptionWithUiText
import com.flixclusive.core.database.dao.provider.InstalledRepositoryDao
import com.flixclusive.core.database.entity.provider.InstalledProvider
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.core.util.log.infoLog
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.domain.downloads.usecase.DownloadFileUseCase
import com.flixclusive.domain.provider.R
import com.flixclusive.domain.provider.usecase.manage.DownloadProviderResult
import com.flixclusive.domain.provider.usecase.manage.InstallProviderUseCase
import com.flixclusive.domain.provider.util.extensions.createFileForProvider
import com.flixclusive.domain.provider.util.extensions.downloadProvider
import com.flixclusive.domain.provider.util.extensions.toInstalledRepository
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.model.provider.Repository.Companion.toValidRepositoryLink
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

internal class InstallProviderUseCaseImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val userSessionDataStore: UserSessionDataStore,
    private val providerRepository: ProviderRepository,
    private val installedRepositoryDao: InstalledRepositoryDao,
    private val downloadFile: DownloadFileUseCase,
    private val appDispatchers: AppDispatchers,
) : InstallProviderUseCase {
    override fun invoke(metadata: ProviderMetadata): Flow<DownloadProviderResult> =
        channelFlow {
            val userId = userSessionDataStore.currentUserId.filterNotNull().first()
            val file = context.createFileForProvider(
                provider = metadata,
                userId = userId,
            )

            val alreadyInstalled = providerRepository.getProvider(id = metadata.id, ownerId = userId) != null
            if (alreadyInstalled) {
                send(
                    DownloadProviderResult.Failure(
                        IllegalStateException(
                            context.getString(R.string.provider_already_exists, metadata.name)
                        ),
                    ),
                )
                return@channelFlow
            }

            try {
                downloadFile.downloadProvider(
                    file = file,
                    metadata = metadata,
                    onStateChange = { state ->
                        val isFinished = state.status.isFinished

                        when {
                            isFinished && state.error != null -> throw ExceptionWithUiText(state.error)
                            isFinished -> infoLog("Downloaded: ${state.file?.absolutePath ?: "unknown file path"}")
                        }

                        trySend(
                            DownloadProviderResult.Downloading(
                                progress = state.progress,
                                downloadId = state.id,
                            )
                        )
                    },
                )
            } catch (e: Throwable) {
                errorLog("Failed to download provider: ${metadata.name}")
                errorLog(e.cause)

                send(
                    DownloadProviderResult.Failure(
                        error = when (e) {
                            is CancellationException -> CancellationException(
                                context.getString(R.string.error_cancelled_provider_download, metadata.name)
                            )

                            is ExceptionWithUiText -> e.cause ?: e

                            else -> e
                        },
                    ),
                )
                return@channelFlow
            }

            val existingRepo = installedRepositoryDao.get(
                url = metadata.repositoryUrl,
                userId = userId
            )
            if (existingRepo == null) {
                infoLog("Repository not found for provider: ${metadata.name}, creating new repository entry")

                val repo = metadata.repositoryUrl.toValidRepositoryLink()
                val newRepo = repo.toInstalledRepository(userId)
                installedRepositoryDao.insert(newRepo)
            }

            val installedProvider = InstalledProvider(
                ownerId = userId,
                id = metadata.id,
                filePath = file.absolutePath,
                repositoryUrl = metadata.repositoryUrl,
            )

            providerRepository.install(installedProvider, metadata)
            send(DownloadProviderResult.Success)
        }.flowOn(appDispatchers.io)
}
