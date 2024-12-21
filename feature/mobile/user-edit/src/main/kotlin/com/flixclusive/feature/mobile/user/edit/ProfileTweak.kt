package com.flixclusive.feature.mobile.user.edit

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter

internal interface ProfileTweak {
    @Composable
    fun getLabel(): String

    @Composable
    fun getTweaks(): List<ProfileTweakUI<*>>
}

internal sealed class ProfileTweakUI<T> {
    open val icon: Painter? = null
    abstract val onValueChange: (T) -> Unit

    /**
     * A text field that can be edited
     * */
    data class TextField(
        val value: String,
        override val onValueChange: (String) -> Unit
    ) : ProfileTweakUI<String>()

    /**
     * A button to click
     * */
    open class Button(
        open val label: String,
        override val icon: Painter,
        open val onClick: () -> Unit,
    ) : ProfileTweakUI<Unit>() {
        override val onValueChange: (Unit) -> Unit = {}
    }

    /**
     * A button that shows a dialog
     * */
    data class Dialog(
        override val label: String,
        override val icon: Painter,
        override val onClick: () -> Unit,
        val content: @Composable () -> Unit
    ) : Button(
        label = label,
        icon = icon,
        onClick = onClick
    )

    // Add more if needed ...
}