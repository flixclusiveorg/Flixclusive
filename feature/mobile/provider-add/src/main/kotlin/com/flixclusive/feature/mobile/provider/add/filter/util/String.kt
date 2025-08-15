package com.flixclusive.feature.mobile.provider.add.filter.util

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.flixclusive.core.strings.UiText
import com.flixclusive.feature.mobile.provider.add.filter.SortableProperty
import com.flixclusive.provider.filter.Filter.Select.Companion.getOptionName

@Composable
internal fun <T> T.toOptionString(): String {
    return when (this) {
        is SortableProperty -> toString(LocalContext.current)
        is UiText -> asString()
        is String -> this
        else -> getOptionName(this)
    }
}

internal fun <T> T.toOptionString(context: Context): String {
    return when (this) {
        is UiText -> asString(context)
        is String -> this
        else -> getOptionName(this)
    }
}
