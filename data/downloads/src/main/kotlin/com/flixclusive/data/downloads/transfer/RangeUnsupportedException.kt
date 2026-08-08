package com.flixclusive.data.downloads.transfer

import java.io.IOException

/**
 * Thrown when a server answers a ranged request with the whole file (`200`) instead of the slice
 * asked for (`206`). Distinct from a generic transfer failure because retrying is pointless — the
 * server will answer the same way every time — and because the fix is to re-plan the download as a
 * single chunk rather than to blame the link.
 */
class RangeUnsupportedException(
    url: String,
) : IOException("Server ignored the Range request and returned the whole file: $url")
