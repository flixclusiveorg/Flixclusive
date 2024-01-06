package com.flixclusive.model.datastore.player

import android.annotation.SuppressLint
import android.graphics.Color


@Suppress("EnumEntryName")
@SuppressLint("UnsafeOptInUsageError")
enum class CaptionEdgeTypePreference(
    val type: Int,
    val color: Int = Color.BLACK
) {
    Drop_Shadow(2),
    Outline(1);
}