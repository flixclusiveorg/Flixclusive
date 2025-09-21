package com.flixclusive.core.common.provider.extensions

import java.util.regex.Pattern

/**
 * Extracts the owner and repository name from a GitHub URL.
 * */
fun String.toOwnerAndRepository(): Pair<String?, String?>? {
    // Support URLs with https://, http://, or no protocol
    val pattern =
        Pattern.compile("(?:https?://)?(?:www\\.)?(github\\.com|raw\\.githubusercontent\\.com)/([^/]+)/([^/]+)(?:/.*)?")
    val matcher = pattern.matcher(this)

    if (matcher.matches()) {
        return matcher.group(2) to matcher.group(3)
    }
    return null
}
