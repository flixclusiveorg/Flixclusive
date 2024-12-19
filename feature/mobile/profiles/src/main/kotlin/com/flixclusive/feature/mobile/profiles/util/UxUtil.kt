package com.flixclusive.feature.mobile.profiles.util

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.unit.IntOffset

internal object UxUtil {
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