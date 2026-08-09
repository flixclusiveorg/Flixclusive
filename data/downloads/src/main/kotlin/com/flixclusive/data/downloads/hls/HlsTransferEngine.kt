package com.flixclusive.data.downloads.hls

import com.flixclusive.data.downloads.transfer.MediaTransferResult
import com.hippo.unifile.UniFile

internal interface HlsTransferEngine {
    /**
     * Downloads [segments] starting at [startIndex] (for resume — segments before it are assumed
     * already written to [destinationFile]), writing them in order regardless of the order they
     * finish downloading in.
     *
     * @param onSegmentWritten called with the absolute count of contiguously-written segments and
     * the total segment count, for progress reporting.
     */
    suspend fun transfer(
        segments: List<HlsSegmentInfo>,
        startIndex: Int,
        headers: Map<String, String>,
        destinationFile: UniFile,
        shouldInterrupt: () -> Boolean,
        onSegmentWritten: suspend (segmentsWritten: Int, totalSegments: Int) -> Unit,
    ): MediaTransferResult
}
