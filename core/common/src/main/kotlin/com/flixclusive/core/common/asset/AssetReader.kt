package com.flixclusive.core.common.asset

/**
 * A generic interface for reading asset files.
 * */
interface AssetReader<T> {
    /**
     * Reads the content of an asset file as a string.
     *
     * @param filePath The path to the asset file.
     * @return The content of the asset file as a string.
     */
    suspend fun read(filePath: String): T
}
