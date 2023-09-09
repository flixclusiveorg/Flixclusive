package com.flixclusive.presentation.mobile.common.transitions

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavBackStackEntry
import com.ramcosta.composedestinations.spec.DestinationStyleAnimated

@OptIn(ExperimentalAnimationApi::class)
object CommonScreenTransition : DestinationStyleAnimated {
    override fun AnimatedContentTransitionScope<NavBackStackEntry>.enterTransition(): EnterTransition = slideInHorizontally { 1000 } + fadeIn()

    override fun AnimatedContentTransitionScope<NavBackStackEntry>.exitTransition(): ExitTransition = fadeOut() + slideOutHorizontally { 1000 }
}