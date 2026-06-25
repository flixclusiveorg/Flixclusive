package com.flixclusive.core.presentation.mobile.extensions

import com.flixclusive.core.drawables.R as UiCommonR

fun getAvatarResource(imageIndex: Int): Int {
    val list = listOf(
        UiCommonR.drawable.avatar0,
        UiCommonR.drawable.avatar1,
        UiCommonR.drawable.avatar2,
        UiCommonR.drawable.avatar3,
        UiCommonR.drawable.avatar4,
        UiCommonR.drawable.avatar5,
        UiCommonR.drawable.avatar6,
        UiCommonR.drawable.avatar7,
        UiCommonR.drawable.avatar8,
        UiCommonR.drawable.avatar9,
        UiCommonR.drawable.avatar10,
        UiCommonR.drawable.avatar11,
        UiCommonR.drawable.avatar12,
        UiCommonR.drawable.avatar13,
    )

    return list[imageIndex % list.size]
}
