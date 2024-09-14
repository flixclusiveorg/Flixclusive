package com.flixclusive.domain.provider.util

import com.flixclusive.core.util.network.okhttp.request
import okhttp3.OkHttpClient
import java.util.regex.Pattern

fun extractGithubInfoFromLink(url: String): Pair<String?, String?>? {
    val pattern = Pattern.compile("https://(github\\.com|raw\\.githubusercontent\\.com)/([^/]+)/([^/]+)(.*?)(?!\\n)")
    val matcher = pattern.matcher(url)

    if (matcher.matches()) {
        return matcher.group(2) to matcher.group(3)
    }
    return null
}

internal fun OkHttpClient.isProviderBranchValid(branchUrl: String): Boolean {
    val response = request(branchUrl).execute()

    return response.isSuccessful
}