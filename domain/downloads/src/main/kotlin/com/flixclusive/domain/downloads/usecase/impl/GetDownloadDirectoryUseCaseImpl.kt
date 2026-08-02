package com.flixclusive.domain.downloads.usecase.impl

import android.content.Context
import androidx.core.net.toUri
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.data.downloads.directory.DownloadDirectoryRepository
import com.flixclusive.domain.downloads.usecase.GetDownloadDirectoryUseCase
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.common.tv.Episode
import com.hippo.unifile.UniFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject

internal class GetDownloadDirectoryUseCaseImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dataStoreManager: DataStoreManager,
    private val downloadDirectoryRepository: DownloadDirectoryRepository,
) : GetDownloadDirectoryUseCase {
    override suspend fun invoke(
        media: MediaMetadata,
        episode: Episode?,
    ): UniFile? {
        val storageDirectoryUri = dataStoreManager.getSystemPrefs().first().storageDirectoryUri ?: return null

        val root = UniFile.fromUri(context, storageDirectoryUri.toUri()) ?: return null
        if (!root.isDirectory || !root.canWrite()) return null

        return downloadDirectoryRepository.getOrCreateMediaDirectory(root, media, episode)
    }
}
