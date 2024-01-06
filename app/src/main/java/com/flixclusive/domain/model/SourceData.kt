package com.flixclusive.domain.model

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import java.io.Serializable

data class SourceData(
    val mediaId: String = "",
    val sourceNameUsed: String = "",
    val cachedLinks: SnapshotStateList<SourceLink> = mutableStateListOf(),
    val cachedSubtitles: SnapshotStateList<Subtitle> = mutableStateListOf()
) : Serializable