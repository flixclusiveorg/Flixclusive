package com.flixclusive.domain.provider.usecase.manage

import com.flixclusive.model.provider.ProviderMetadata
import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Use case for loading provider metadata and optionally downloading the provider file.
 *
 */
interface LoadProviderUseCase {
    /**
     * Downloads and loads the provider metadata.
     *
     * @param metadata The metadata of the provider to download and load.
     *
     * @return A flow of [LoadProviderResult] containing the results of the loading operation.
     * */
    operator fun invoke(metadata: ProviderMetadata): Flow<LoadProviderResult>

    /**
     * Loads the provider metadata from a file path.
     *
     * @param metadata The metadata of the provider to load.
     * @param filePath The file path of the provider to load from.
     *
     * @return A flow of [LoadProviderResult] containing the results of the loading operation.
     * */
    operator fun invoke(
        metadata: ProviderMetadata,
        filePath: String,
    ): Flow<LoadProviderResult>
}

/**
 * Represents the result of loading provider metadata.
 * */
sealed class LoadProviderResult {
    /**
     * Represents a successful loading of provider metadata.
     *
     * @property provider The metadata of the successfully loaded provider.
     * */
    data class Success(
        val provider: ProviderMetadata,
    ) : LoadProviderResult()

    /**
     * Represents a failure in loading provider metadata.
     *
     * @param provider The metadata of the provider that failed to load.
     * @param filePath The file path of the provider that failed to load, if applicable.
     * @param error The error that occurred during the loading process.
     *
     * @property isFileDownloaded Indicates whether the provider file was successfully downloaded.
     * If the file path is not null, it checks if the file exists at that path
     * */
    data class Failure(
        val provider: ProviderMetadata,
        val filePath: String,
        val error: Throwable,
    ) : LoadProviderResult() {
        val isFileDownloaded: Boolean get() = File(filePath).exists()
    }
}
