package com.flixclusive.domain.provider.util

import androidx.annotation.StringRes
import com.flixclusive.core.locale.UiText

internal object StringHelper {
    fun createString(string: String): UiText.StringValue {
        return UiText.StringValue(string)
    }

    fun getString(
        @StringRes resId: Int,
        vararg args: Any,
    ): UiText.StringResource {
        return UiText.StringResource(resId, *args)
    }
}