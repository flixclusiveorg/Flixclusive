package com.flixclusive.feature.mobile.user.edit.tweaks

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.strings.UiText
import com.flixclusive.core.ui.common.util.IconResource
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveModifierUtil.fillMaxAdaptiveWidth
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveStylesUtil.getAdaptiveTextStyle
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveUiUtil.getAdaptiveDp
import com.flixclusive.core.ui.common.util.adaptive.TextStyleMode
import com.flixclusive.core.ui.common.util.adaptive.TypographyStyle
import com.flixclusive.core.ui.common.util.onMediumEmphasis
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
        override val onValueChange: (String) -> Unit
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
            onDismiss: () -> Unit
        ) -> Unit
    ) : Button(
        label = label,
        description =  description,
        needsConfirmation = false,
        icon = icon,
        onClick = {}
    )

    // Add more if needed ...
}

internal fun LazyListScope.renderTweakUi(
    tweakCategory: BaseProfileTweak
) {
    item {
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier
                .fillMaxAdaptiveWidth()
                .padding(top = getAdaptiveDp(25.dp))
        ) {
            Text(
                text = tweakCategory.getLabel(),
                style = getAdaptiveTextStyle(
                    size = 16.sp,
                    style = TypographyStyle.Label,
                    mode = TextStyleMode.Emphasized,
                ).copy(
                    color = LocalContentColor.current.onMediumEmphasis(0.8F)
                )
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
