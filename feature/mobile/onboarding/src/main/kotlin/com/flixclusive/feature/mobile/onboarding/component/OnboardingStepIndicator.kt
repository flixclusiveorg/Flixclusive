package com.flixclusive.feature.mobile.onboarding.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
internal fun OnboardingStepIndicator(
    currentIndex: Int,
    steps: Int,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
    height: Dp = 8.dp,
    activeWidth: Dp = 24.dp,
    inactiveWidth: Dp = 8.dp,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier,
    ) {
        repeat(steps) { index ->
            val isActive = index == currentIndex

            val width by animateDpAsState(
                targetValue = if (isActive) activeWidth else inactiveWidth,
                animationSpec = tween(durationMillis = 250),
                label = "onboarding_step_indicator_width",
            )

            val color by animateColorAsState(
                targetValue = if (isActive) activeColor else inactiveColor,
                animationSpec = tween(durationMillis = 250),
                label = "onboarding_step_indicator_color",
            )

            Box(
                modifier = Modifier
                    .height(height)
                    .width(width)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(color),
            )
        }
    }
}
