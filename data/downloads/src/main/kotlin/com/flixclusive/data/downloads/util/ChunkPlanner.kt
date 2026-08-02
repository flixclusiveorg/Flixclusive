package com.flixclusive.data.downloads.util

object ChunkPlanner {
    const val OPEN_ENDED_RANGE_END = -1L

    fun plan(totalBytes: Long?): List<LongRange> {
        if (totalBytes == null || totalBytes <= 0) {
            return listOf(0L..OPEN_ENDED_RANGE_END)
        }

        val chunkCount = (totalBytes / MIN_CHUNK_SIZE_BYTES).toInt().coerceIn(1, MAX_CHUNKS)
        val baseSize = totalBytes / chunkCount

        var start = 0L
        return List(chunkCount) { index ->
            val end = if (index == chunkCount - 1) totalBytes - 1 else start + baseSize - 1
            (start..end).also { start = end + 1 }
        }
    }

    private const val MAX_CHUNKS = 4
    private const val MIN_CHUNK_SIZE_BYTES = 2L * 1024 * 1024
}
