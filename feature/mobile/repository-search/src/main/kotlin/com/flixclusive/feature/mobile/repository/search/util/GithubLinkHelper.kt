package com.flixclusive.feature.mobile.repository.search.util

import java.util.regex.Pattern

internal fun parseGithubUrl(url: String): String? {
    val (username, repository) = extractGithubInfoFromLink(url) ?: return null

    return "https://github.com/$username/$repository"
}

internal fun extractGithubInfoFromLink(url: String): Pair<String?, String?>? {
    val pattern = Pattern.compile(
        "https://(github\\.com|raw\\.githubusercontent\\.com)/(?<username>[^/]+)/(?<repository>[^/]+)(.*?)(?!\\n)"
    )
    val matcher = pattern.matcher(url)

    if (matcher.matches()) {
        return matcher.group(2) to matcher.group(3)
    }
    return null
}