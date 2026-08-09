package com.flixclusive.core.datastore.model.user.download

/**
 * Orthogonal to [DownloadLinkSelectionMode]: which end of the chosen axis to prefer.
 *
 * - For [DownloadLinkSelectionMode.SIZE_FIRST], this directly picks the sort order (largest or
 *   smallest known content length first).
 * - For [DownloadLinkSelectionMode.QUALITY_FIRST], the primary sort stays "closest to the
 *   player's preferred quality" — this only decides which way ties/wraps break when nothing
 *   matches the preferred tier exactly (step up towards a higher tier first, or down towards a
 *   lower one first).
 */
enum class DownloadLinkSortDirection {
    HIGHEST_FIRST,
    LOWEST_FIRST,
}
