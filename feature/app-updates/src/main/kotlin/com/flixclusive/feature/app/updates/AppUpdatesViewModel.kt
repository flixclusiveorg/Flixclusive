package com.flixclusive.feature.app.updates

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.common.config.BuildConfigProvider
import com.flixclusive.core.common.exception.ExceptionWithUiText
import com.flixclusive.core.common.file.LegacyExternalStorageUtil
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.data.app.updates.model.AppUpdateInfo
import com.flixclusive.data.app.updates.repository.AppUpdatesRepository
import com.flixclusive.data.downloads.model.DownloadState
import com.flixclusive.data.downloads.model.DownloadState.Companion.error
import com.flixclusive.domain.downloads.model.DownloadRequest
import com.flixclusive.domain.downloads.model.DownloadRequest.Companion.getDownloadId
import com.flixclusive.domain.downloads.usecase.CancelDownloadUseCase
import com.flixclusive.domain.downloads.usecase.DownloadFileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class AppUpdatesViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val appUpdatesRepository: AppUpdatesRepository,
        private val downloadFile: DownloadFileUseCase,
        private val _cancelDownload: CancelDownloadUseCase,
        private val buildConfigProvider: BuildConfigProvider,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<AppUpdatesUiState>(AppUpdatesUiState.Loading)
        val uiState = _uiState.asStateFlow()

        private val _downloadState = MutableStateFlow(DownloadState.IDLE)
        val downloadState = _downloadState.asStateFlow()

        private var downloadJob: Job? = null

        val applicationId get() = buildConfigProvider.get().applicationId

        init {
            checkForUpdates()
        }

        fun checkForUpdates() {
            viewModelScope.launch {
                _uiState.update { AppUpdatesUiState.Loading }

                appUpdatesRepository
                    .getLatestUpdate()
                    .onSuccess { appUpdateInfo ->
                        if (appUpdateInfo != null) {
                            _uiState.update { AppUpdatesUiState.UpdateAvailable(appUpdateInfo) }
                        } else {
                            _uiState.update { AppUpdatesUiState.UpToDate }
                        }
                    }.onFailure { error ->
                        _uiState.update {
                            AppUpdatesUiState.Error((error as ExceptionWithUiText).uiText!!)
                        }
                    }
            }
        }

        /**
         * Cancels an ongoing download.
         *
         * @param version The version of the update being downloaded.
         * @param url The URL from which the update is being downloaded.
         * */
        fun cancelDownload(
            version: String,
            url: String,
        ) {
            // No download in progress
            if (downloadJob?.isActive == false) return

            val destinationPath = context.externalCacheDir
                ?: LegacyExternalStorageUtil.getPublicDownloadsDirectory()

            requireNotNull(destinationPath) {
                "Failed to access storage to cancel download."
            }

            val downloadId = getDownloadId(
                url = url,
                fileName = "$version.apk",
                destinationPath = destinationPath.absolutePath,
            )

            _cancelDownload(downloadId)
            downloadJob?.cancel()
            downloadJob = null
            _downloadState.update { DownloadState.IDLE }
        }

        /**
         * Downloads the update APK file.
         *
         * @param version The version of the update to be downloaded.
         * @param url The URL from which to download the update.
         * */
        fun downloadUpdate(
            version: String,
            url: String,
        ) {
            // Download already in progress
            if (downloadJob?.isActive == true) return

            downloadJob = viewModelScope.launch {
                val destinationPath = context.externalCacheDir
                    ?: LegacyExternalStorageUtil.getPublicDownloadsDirectory()

                if (destinationPath == null) {
                    _downloadState.update { it.error(UiText.from(R.string.failed_to_access_storage)) }
                    return@launch
                }

                val file = File(destinationPath, "$version.apk")

                downloadFile(
                    request = DownloadRequest.from(
                        url = url,
                        fileName = file.name,
                        destinationPath = file.parent!!,
                    ),
                ).collectLatest {
                    _downloadState.value = it
                }
            }
        }
    }

sealed class AppUpdatesUiState {
    @Immutable
    data object Loading : AppUpdatesUiState()

    @Immutable
    data object UpToDate : AppUpdatesUiState()

    @Immutable
    data class UpdateAvailable(
        val updateInfo: AppUpdateInfo,
    ) : AppUpdatesUiState()

    @Immutable
    data class Error(
        val message: UiText,
    ) : AppUpdatesUiState()
}
