package com.flixclusive.core.network.util.okhttp

import com.flixclusive.core.util.network.okhttp.USER_AGENT
import com.flixclusive.core.util.network.json.fromJson
import com.flixclusive.core.util.network.okhttp.request
import okhttp3.OkHttpClient
import kotlin.random.Random

class UserAgentManager(
    private val client: OkHttpClient
) {
    @Suppress("MemberVisibilityCanBePrivate", "unused")
    companion object {
        val desktopUserAgents = arrayListOf<String>()
        val mobileUserAgents = arrayListOf<String>()

        fun getRandomUserAgent(): String {
            val isDesktop = Random.nextBoolean()

            return when {
                isDesktop -> desktopUserAgents.randomOrNull()
                else -> mobileUserAgents.randomOrNull()
            } ?: USER_AGENT
        }

        fun getRandomMobileUserAgent(): String {
            return mobileUserAgents.randomOrNull() ?: USER_AGENT
        }

        fun getRandomDesktopUserAgent(): String {
            return desktopUserAgents.randomOrNull() ?: USER_AGENT
        }
    }

    private val userAgentSourceUrl
        = "https://flixclusiveorg.github.io/user-agents/user-agents.min.json"

    fun loadLatestUserAgents() {
        val desktopKey = "desktop"
        val mobileKey = "mobile"

        val userAgents = client.request(
            url = userAgentSourceUrl
        ).execute()
            .fromJson<Map<String, List<String>>>()

        desktopUserAgents.clear()
        mobileUserAgents.clear()

        userAgents[desktopKey]?.let(desktopUserAgents::addAll)
        userAgents[mobileKey]?.let(mobileUserAgents::addAll)
    }
}