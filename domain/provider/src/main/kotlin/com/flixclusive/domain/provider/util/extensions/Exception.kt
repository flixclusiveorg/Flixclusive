package com.flixclusive.domain.provider.util.extensions

import android.content.Context
import com.flixclusive.domain.provider.R
import com.flixclusive.core.strings.R as StringsR

fun Context.getApiCrashMessage(provider: String): String =
    getString(R.string.failed_to_load_provider_on_api, provider)

fun Context.getCommonCrashMessage(provider: String): String =
    getString(StringsR.string.failed_to_load_provider, provider)
