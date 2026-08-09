package com.flixclusive.feature.mobile.settings.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.flixclusive.core.drawables.R as UiCommonR

/**
 * A filter chip that grows a check mark in when selected.
 *
 * Note for anyone adopting this on an existing screen: the animated leading icon and the default
 * chip colours are part of the look, so swapping a plain [FilterChip] for this one is a visual
 * change, not a like-for-like refactor.
 */
@Composable
internal fun AnimatedFilterChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        shape = CircleShape,
        leadingIcon = {
            AnimatedVisibility(
                visible = selected,
                enter = fadeIn(tween(150)) + expandHorizontally(tween(150)),
                exit = fadeOut(tween(150)) + shrinkHorizontally(tween(150)),
            ) {
                Icon(
                    painter = painterResource(UiCommonR.drawable.check),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
        },
        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = selected),
        modifier = modifier.animateContentSize(),
    )
}
