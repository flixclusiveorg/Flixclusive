package com.flixclusive.core.datastore.model.user.player

import android.annotation.SuppressLint
import android.graphics.Color
import com.flixclusive.core.datastore.R

@Suppress("EnumEntryName")
@SuppressLint("UnsafeOptInUsageError")
enum class CaptionEdgeTypePreference(
    val type: Int,
    val color: Int = Color.BLACK,
) {
    Drop_Shadow(2),
    Outline(1),
    ;

    fun getStringResId(): Int {
        return when (this) {
            Drop_Shadow -> R.string.drop_shadow
            Outline -> R.string.outline
        }
    }
}
