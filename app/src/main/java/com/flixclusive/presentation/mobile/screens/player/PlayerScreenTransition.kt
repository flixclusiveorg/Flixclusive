package com.flixclusive.presentation.mobile.screens.player

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.navigation.NavBackStackEntry
import com.ramcosta.composedestinations.spec.DestinationStyle

object PlayerScreenTransition : DestinationStyle.Animated {
    override fun AnimatedContentTransitionScope<NavBackStackEntry>.enterTransition() = scaleIn() + fadeIn()

    override fun AnimatedContentTransitionScope<NavBackStackEntry>.exitTransition() = fadeOut()
}