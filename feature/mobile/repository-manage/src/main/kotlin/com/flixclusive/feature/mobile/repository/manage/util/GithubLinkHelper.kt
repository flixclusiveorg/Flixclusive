package com.flixclusive.feature.mobile.repository.manage.util

import com.flixclusive.domain.provider.util.extractGithubInfoFromLink

internal fun parseGithubUrl(url: String): String? {
    val (username, repository) = extractGithubInfoFromLink(url) ?: return null

    return "https://github.com/$username/$repository"
}
