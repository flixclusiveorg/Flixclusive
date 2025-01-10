package com.flixclusive.domain.provider.util

import android.content.Context
import com.flixclusive.core.locale.R as LocaleR

fun Context.getApiCrashMessage(provider: String): String =
    getString(LocaleR.string.failed_to_load_provider_on_api, provider)

fun Context.getCommonCrashMessage(provider: String): String =
    getString(LocaleR.string.failed_to_load_provider, provider)
