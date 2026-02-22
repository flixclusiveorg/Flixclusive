package com.flixclusive.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.navigation.NavBackStackEntry
import com.flixclusive.navigation.extensions.defaultEnterTransition
import com.flixclusive.navigation.extensions.defaultExitTransition
import com.flixclusive.navigation.extensions.defaultPopEnterTransition
import com.flixclusive.navigation.extensions.defaultPopExitTransition
import com.ramcosta.composedestinations.animations.NavHostAnimatedDestinationStyle

@OptIn(ExperimentalAnimationApi::class)
internal object AppDefaultTransition : NavHostAnimatedDestinationStyle() {
    override val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition
        get() = {
            defaultEnterTransition(
                initial = initialState,
                target = targetState,
            )
        }

    override val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition
        get() = {
            defaultExitTransition(
                initial = initialState,
                target = targetState,
            )
        }

    override val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition
        get() = { defaultPopEnterTransition() }

    override val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition
        get() = { defaultPopExitTransition() }
}
