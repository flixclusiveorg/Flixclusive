@file:Suppress("ktlint:compose:compositionlocal-allowlist")

package com.flixclusive.feature.mobile.settings.util

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf

val LocalSettingsSearchQuery = compositionLocalOf<MutableState<String>> { mutableStateOf("") }
