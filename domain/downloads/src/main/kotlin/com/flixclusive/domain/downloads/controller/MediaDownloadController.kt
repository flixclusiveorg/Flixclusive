package com.flixclusive.domain.downloads.controller

interface MediaDownloadController {
    fun start(itemId: String)

    fun pause(itemId: String)

    fun resume(itemId: String)

    fun stop(itemId: String)

    fun retry(itemId: String)

    /**
     * Requeues and re-dispatches every download the process died in the middle of, so an app that
     * was force-closed mid-transfer picks up where it left off instead of leaving rows frozen in a
     * state nothing is driving. Safe to call at any time — items currently being transferred are
     * left alone, and a sweep already in flight is not started twice. A *completed* sweep does not
     * block a later one: the activity fires this on every foreground, and a second sweep simply
     * finds nothing to requeue.
     */
    fun resumeInterrupted()

    fun delete(itemId: String)

    fun pauseBatch(
        mediaId: String,
        seasonNumber: Int,
    )

    fun stopBatch(
        mediaId: String,
        seasonNumber: Int,
    )
}
