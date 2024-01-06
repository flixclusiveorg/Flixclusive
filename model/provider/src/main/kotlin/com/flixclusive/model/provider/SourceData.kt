package com.flixclusive.model.provider

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import java.io.Serializable

/**
 *
 * Data model used by the provider and domain layer which
 * is to be sent to the ui layer for unwrapping.
 *
 * @param mediaId the media id of the film from the used provider
 * @param providerName the name of the provider used
 * @param cachedLinks watchable links obtained from the provider
 * @param cachedSubtitles subtitle links obtained from the provider
 * */
data class SourceData(
    val mediaId: String = "",
    val providerName: String = "",
    val cachedLinks: SnapshotStateList<SourceLink> = mutableStateListOf(),
    val cachedSubtitles: SnapshotStateList<Subtitle> = mutableStateListOf()
) : Serializable