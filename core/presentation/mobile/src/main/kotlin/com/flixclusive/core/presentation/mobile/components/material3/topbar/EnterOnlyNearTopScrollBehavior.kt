package com.flixclusive.core.presentation.mobile.components.material3.topbar

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.TopAppBarState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlin.math.abs

class EnterOnlyNearTopScrollBehavior(
    override val state: TopAppBarState,
    override val snapAnimationSpec: AnimationSpec<Float>?,
    override val flingAnimationSpec: DecayAnimationSpec<Float>?,
    val canScroll: () -> Boolean = { true },
) : TopAppBarScrollBehavior {
    var nearTopThreshold: Float = 200f

    override val isPinned: Boolean = false
    override var nestedScrollConnection =
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (!canScroll()) return Offset.Zero
                val prevHeightOffset = state.heightOffset

                // Only allow expansion when near the top of the scroll layout
                val isScrollingUp = available.y > 0f
                val isNearTop = abs(state.contentOffset) <= nearTopThreshold

                if (isScrollingUp && !isNearTop) {
                    // Don't expand the app bar if scrolling up but not near the top
                    return Offset.Zero
                }

                state.heightOffset = state.heightOffset + available.y
                return if (prevHeightOffset != state.heightOffset) {
                    // We're in the middle of top app bar collapse or expand.
                    // Consume only the scroll on the Y axis.
                    available.copy(x = 0f)
                } else {
                    Offset.Zero
                }
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (!canScroll()) return Offset.Zero
                state.contentOffset += consumed.y
                if (state.heightOffset == 0f || state.heightOffset == state.heightOffsetLimit) {
                    if (consumed.y == 0f && available.y > 0f) {
                        // Reset the total content offset to zero when scrolling all the way down.
                        // This will eliminate some float precision inaccuracies.
                        state.contentOffset = 0f
                    }
                }

                // Only allow expansion when near the top, but always allow collapse
                val isScrollingUp = consumed.y > 0f
                val isNearTop = abs(state.contentOffset) <= nearTopThreshold

                if (isScrollingUp && !isNearTop) {
                    // Don't expand the app bar if scrolling up but not near the top
                    return Offset.Zero
                }

                state.heightOffset = state.heightOffset + consumed.y
                return Offset.Zero
            }

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity,
            ): Velocity {
                val superConsumed = super.onPostFling(consumed, available)
                return superConsumed +
                    settleAppBar(state, available.y, flingAnimationSpec, snapAnimationSpec)
            }
        }
}

@Composable
fun rememberEnterOnlyNearTopScrollBehavior(): EnterOnlyNearTopScrollBehavior {
    val scrollState = rememberTopAppBarState()
    val fling = rememberSplineBasedDecay<Float>()

    return remember {
        EnterOnlyNearTopScrollBehavior(
            state = scrollState,
            snapAnimationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            flingAnimationSpec = fling,
            canScroll = { true },
        )
    }
}
