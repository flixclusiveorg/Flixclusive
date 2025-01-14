package com.flixclusive.domain.provider.util

import java.util.regex.Pattern

fun extractGithubInfoFromLink(url: String): Pair<String?, String?>? {
    val pattern = Pattern.compile("https://(github\\.com|raw\\.githubusercontent\\.com)/([^/]+)/([^/]+)(.*?)(?!\\n)")
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
