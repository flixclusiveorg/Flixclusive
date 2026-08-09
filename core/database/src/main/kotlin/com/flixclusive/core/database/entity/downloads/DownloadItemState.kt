package com.flixclusive.core.database.entity.downloads

enum class DownloadItemState {
    QUEUED,
    DOWNLOADING_STREAM,
    PAUSED,
    STREAM_COMPLETE,
    FETCHING_SUBTITLES,
    COMPLETED,
    STOPPED,
    FAILED,
    ;

    val isTerminal: Boolean get() = this == COMPLETED || this == STOPPED || this == FAILED
}
