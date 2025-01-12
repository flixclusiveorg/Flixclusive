package com.flixclusive.feature.mobile.user.pin.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveStylesUtil.getAdaptiveTextStyle
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveUiUtil.getAdaptiveDp
import com.flixclusive.core.ui.common.util.adaptive.TextStyleMode
import com.flixclusive.core.ui.common.util.adaptive.TypographyStyle

@Composable
internal fun PinPlaceholder(
    showPin: Boolean,
    hasErrors: Boolean,
    char: Char?,
) {
    val size = getAdaptiveDp(16.dp)

    val inputColor by animateColorAsState(
        label = "PinPlaceholderInputColor",
        targetValue =
            if (char != null && hasErrors) {
                MaterialTheme.colorScheme.error
            } else if (char != null) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceColorAtElevation(20.dp)
            },
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement =
            Arrangement.spacedBy(
                space = 5.dp,
                alignment = Alignment.Bottom,
            ),
        modifier =
            Modifier
                .heightIn(size * 4F),
    ) {
        AnimatedVisibility(
            visible = showPin,
            enter = fadeIn() + slideInVertically(tween(100)) { it / 4 },
            exit = fadeOut(),
        ) {
            Text(
                text = "${char ?: ""}",
                style =
                    getAdaptiveTextStyle(
                        style = TypographyStyle.Title,
                        mode = TextStyleMode.Emphasized,
                        size = 25.sp,
                    ),
            )
        }

        Spacer(
            modifier =
                Modifier
                    .size(size)
                    .background(
                        color = inputColor,
                        shape = CircleShape,
                    ),
        )
    }
}
