@file:Suppress("ktlint:compose:compositionlocal-allowlist")

package com.flixclusive.feature.mobile.settings.util

import androidx.compose.runtime.compositionLocalOf

val LocalSettingsSearchQuery = compositionLocalOf<String> { "" }
