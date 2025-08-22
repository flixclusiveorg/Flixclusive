package com.flixclusive.domain.provider.util

import java.util.regex.Pattern

fun extractGithubInfoFromLink(url: String): Pair<String?, String?>? {
    // Support URLs with https://, http://, or no protocol
    val pattern =
        Pattern.compile("(?:https?://)?(?:www\\.)?(github\\.com|raw\\.githubusercontent\\.com)/([^/]+)/([^/]+)(?:/.*)?")
    val matcher = pattern.matcher(url)

    if (matcher.matches()) {
        return matcher.group(2) to matcher.group(3)
    }
    return null
}

fun String.toGithubUrl(): String? {
    val (username, repository) = extractGithubInfoFromLink(this) ?: return null

    return "https://github.com/$username/$repository"
}
