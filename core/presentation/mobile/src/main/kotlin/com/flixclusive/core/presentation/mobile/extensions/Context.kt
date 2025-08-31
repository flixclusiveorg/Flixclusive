package com.flixclusive.core.presentation.mobile.extensions

import android.annotation.SuppressLint
import android.content.Context
import com.flixclusive.core.presentation.mobile.components.UserAvatarDefaults.AVATAR_PREFIX

@SuppressLint("DiscouragedApi")
fun Context.getAvatarResource(imageIndex: Int): Int {
    val resourceName = "$AVATAR_PREFIX$imageIndex"
    val id = resources.getIdentifier(resourceName, "drawable", packageName)

    require(id != 0) {
        "Avatar image could not be found: avatar$imageIndex"
    }

    return id
}
