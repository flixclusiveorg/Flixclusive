package com.flixclusive.feature.mobile.settings.screen.system.logcat

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * [entries], [matchIndices] and [maxLineLength] are produced together in a single mapping step.
 * They have to be: indices that arrive separately from the list they point into can be read between
 * two recompositions and index out of bounds.
 */
@Immutable
internal data class LogcatUiState(
    val entries: ImmutableList<LogEntry> = persistentListOf(),
    val matchIndices: ImmutableList<Int> = persistentListOf(),
    val maxLineLength: Int = 0,
    val searchQuery: String = "",
    val totalCount: Int = 0,
    val filterError: String? = null,
)
