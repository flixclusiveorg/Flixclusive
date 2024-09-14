package com.flixclusive.data.provider.util

import android.content.Context
import com.flixclusive.core.locale.R as LocaleR

object CrashHelper {
    fun isCrashingOnGetApiMethod(e: Throwable): Boolean {
        return e.stackTrace.any { it.methodName.equals("getApi") }
    }

    fun Context.getApiCrashMessage(provider: String): String
        = getString(LocaleR.string.failed_to_load_provider_on_api, provider)
}