package com.flixclusive.feature.mobile.library.manage.component

import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.IndicatorBox
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.flixclusive.core.presentation.common.components.GradientCircularProgressIndicator
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

@Composable
internal fun RefreshIndicator(
    refreshState: PullToRefreshState,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier,
) {
    IndicatorBox(
        state = refreshState,
        isRefreshing = isRefreshing,
        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
        modifier = modifier,
    ) {
        GradientCircularProgressIndicator(
            thickness = 2.5.dp,
            colors = listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.tertiary,
            ),
            modifier = Modifier
                .graphicsLayer {
                    val progress = refreshState.distanceFraction

                    // Discard first 40% of progress. Scale remaining progress to full range between 0 and 100%.
                    val adjustedPercent = max(min(1f, progress) - 0.4f, 0f) * 5 / 3
                    // How far beyond the threshold pull has gone, as a percentage of the threshold.
                    val overshootPercent = abs(progress) - 1.0f
                    // Limit the overshoot to 200%. Linear between 0 and 200.
                    val linearTension = overshootPercent.coerceIn(0f, 2f)
                    // Non-linear tension. Increases with linearTension, but at a decreasing rate.
                    val tensionPercent = linearTension - linearTension.pow(2) / 4

                    alpha = if (isRefreshing) 1f else (-0.25f + 0.4f * adjustedPercent + tensionPercent) * 0.5f
                }
                .size(16.dp),
        )
    }
}

