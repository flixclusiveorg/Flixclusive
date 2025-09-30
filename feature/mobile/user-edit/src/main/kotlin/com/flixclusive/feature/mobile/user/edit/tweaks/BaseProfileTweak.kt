package com.flixclusive.feature.mobile.user.edit.tweaks

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.presentation.common.util.IconResource
import com.flixclusive.core.presentation.mobile.util.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.core.presentation.mobile.extensions.fillMaxAdaptiveWidth
import com.flixclusive.core.presentation.mobile.util.AdaptiveSizeUtil.getAdaptiveDp
import com.flixclusive.feature.mobile.user.edit.tweaks.component.TweakButton
import com.flixclusive.feature.mobile.user.edit.tweaks.component.TweakTextField

internal interface BaseProfileTweak {
    @Composable
    fun getLabel(): String

    fun getTweaks(): List<ProfileTweakUI<*>>
}

internal sealed class ProfileTweakUI<T> {
    open val icon: IconResource? = null
    abstract val onValueChange: (T) -> Unit

    /**
     * A text field that can be edited
     * */
    data class TextField(
        val initialValue: String,
        val placeholder: UiText? = null,
        override val onValueChange: (String) -> Unit,
    ) : ProfileTweakUI<String>()

    /**
     * A button to click
     * */
    open class Button(
        open val label: UiText,
        open val description: UiText,
        override val icon: IconResource,
        val needsConfirmation: Boolean = false,
        val onClick: () -> Unit,
    ) : ProfileTweakUI<Unit>() {
        override val onValueChange: (Unit) -> Unit = {}
    }

    /**
     * A button that shows a dialog
     * */
    data class Dialog(
        override val label: UiText,
        override val description: UiText,
        override val icon: IconResource,
        val content: @Composable (
            onDismiss: () -> Unit,
        ) -> Unit,
    ) : Button(
            label = label,
            description = description,
            needsConfirmation = false,
            icon = icon,
            onClick = {},
    )

    // Add more if needed ...
}

internal fun LazyListScope.renderTweakUi(tweakCategory: BaseProfileTweak) {
    item {
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier
                .fillMaxAdaptiveWidth()
                .padding(top = getAdaptiveDp(25.dp)),
        ) {
            Text(
                text = tweakCategory.getLabel(),
                color = LocalContentColor.current.copy(0.8F),
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.labelLarge.asAdaptiveTextStyle(),
            )
        }
    }

    items(tweakCategory.getTweaks()) { tweak ->
        when (tweak) {
            is ProfileTweakUI.Button -> TweakButton(tweak)
            is ProfileTweakUI.TextField -> TweakTextField(tweak)
        }
    }
}
