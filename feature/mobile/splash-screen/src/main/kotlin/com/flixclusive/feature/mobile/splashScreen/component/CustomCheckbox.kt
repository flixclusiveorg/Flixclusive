package com.flixclusive.feature.mobile.splashScreen.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.flixclusive.core.ui.mobile.util.onMediumEmphasis


/**
 *
 *
 * Based on: [this](https://stackoverflow.com/a/74129747/19371763)
 * */
@Composable
fun CustomCheckbox(
    modifier: Modifier = Modifier,
    checked: Boolean,
    checkedColor: Color,
    uncheckedColor: Color,
    size: Dp = 24.dp,
    onCheckedChange: (Boolean) -> Unit,
    labelContent: (@Composable () -> Unit)? = null,
) {
    val checkboxColor: Color by animateColorAsState(if (checked) checkedColor else uncheckedColor,
        label = ""
    )
    val density = LocalDensity.current
    val duration = 200

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = modifier
            .heightIn(min = 25.dp)
            .toggleable(
                value = checked,
                role = Role.Checkbox,
                onValueChange = onCheckedChange
            )
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .background(color = checkboxColor, shape = RoundedCornerShape(4.dp))
                .border(
                    width = 1.5.dp,
                    color = checkedColor.onMediumEmphasis(),
                    shape = RoundedCornerShape(4.dp)
                )
                .toggleable(
                    value = checked,
                    role = Role.Checkbox,
                    onValueChange = onCheckedChange
                ),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = checked,
                enter = slideInHorizontally(animationSpec = tween(duration)) {
                    with(density) { size.minus(0.5.dp).roundToPx() }
                } + expandHorizontally(
                    expandFrom = Alignment.Start,
                    animationSpec = tween(duration)
                ),
                exit = fadeOut()
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = uncheckedColor
                )
            }
        }

        labelContent?.invoke()
    }
}