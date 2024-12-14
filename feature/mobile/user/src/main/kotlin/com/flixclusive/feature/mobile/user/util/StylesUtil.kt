package com.flixclusive.feature.mobile.user.util

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.flixclusive.core.ui.common.util.onMediumEmphasis

internal object StylesUtil {
    @Composable
    fun getNonEmphasizedLabel(fontSize: TextUnit = 14.sp): TextStyle {
        return MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.Normal,
            color = LocalContentColor.current.onMediumEmphasis(),
            fontSize = fontSize
        )
    }

    fun <S> AnimatedContentTransitionScope<S>.getSlidingTransition(
        isSlidingRight: Boolean
    ): ContentTransform {
        val tweenInt = tween<IntOffset>(durationMillis = 300)
        val tweenFloat = tween<Float>(durationMillis = 500)

        return if (isSlidingRight) {
            fadeIn(tweenFloat) + slideInHorizontally(animationSpec = tweenInt) { it } togetherWith
                    fadeOut() + slideOutHorizontally { -it / 2 }
        } else {
            fadeIn(tweenFloat) + slideInHorizontally(tweenInt) { -it } + fadeIn() togetherWith
                    fadeOut() + slideOutHorizontally { it / 2 }
        }.using(
            SizeTransform(clip = false)
        )
    }
}