package com.flixclusive.feature.mobile.searchExpanded.component.filter.util

import android.content.Context
import androidx.compose.runtime.Composable
import com.flixclusive.core.locale.UiText
import com.flixclusive.provider.filter.Filter.Select.Companion.getOptionName

@Composable
internal fun <T> T.toOptionString(): String {
   return when (this) {
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