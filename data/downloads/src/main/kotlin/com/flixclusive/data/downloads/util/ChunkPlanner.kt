package com.flixclusive.data.downloads.util

internal object ChunkPlanner {
    const val OPEN_ENDED_RANGE_END = -1L

    fun plan(totalBytes: Long?): List<LongRange> {
        if (totalBytes == null || totalBytes <= 0) {
            return listOf(0L..OPEN_ENDED_RANGE_END)
        }

        // Clamped before narrowing: dividing first happens to stay in Int range for a 2 MiB
        // divisor, but the order that cannot overflow is the one to write down.
        val chunkCount = (totalBytes / MIN_CHUNK_SIZE_BYTES).coerceIn(1L, MAX_CHUNKS.toLong()).toInt()
        val baseSize = totalBytes / chunkCount

        // Every chunk is baseSize except the last, which absorbs the remainder -- so each range is
        // a pure function of its index and needs no running cursor.
        return List(chunkCount) { index ->
            val start = index * baseSize
            val end = if (index == chunkCount - 1) totalBytes - 1 else start + baseSize - 1
            start..end
        }
    }

    private const val MAX_CHUNKS = 4
    private const val MIN_CHUNK_SIZE_BYTES = 2L * 1024 * 1024
}
