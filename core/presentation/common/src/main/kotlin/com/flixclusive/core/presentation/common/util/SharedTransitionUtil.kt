package com.flixclusive.core.presentation.common.util

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf

@OptIn(ExperimentalSharedTransitionApi::class)
object SharedTransitionUtil {
    @Suppress("ktlint:compose:compositionlocal-allowlist")
    val LocalAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

    @Suppress("ktlint:compose:compositionlocal-allowlist")
    val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

    @Composable
    fun getLocalSharedTransitionScope(): SharedTransitionScope {
        return LocalSharedTransitionScope.current
            ?: error("No SharedTransitionScope provided")
    }

    @Composable
    fun getLocalAnimatedVisibilityScope(): AnimatedVisibilityScope {
        return LocalAnimatedVisibilityScope.current
            ?: error("No LocalAnimatedVisibilityScope provided")
    }

    @Composable
    fun ProvideSharedTransitionScope(content: @Composable SharedTransitionScope.() -> Unit) {
        SharedTransitionLayout {
            CompositionLocalProvider(
                LocalSharedTransitionScope provides this,
                content = { content() },
            )
        }
    }

    @Composable
    fun AnimatedVisibilityScope.ProvideAnimatedVisibilityScope(
        content: @Composable AnimatedVisibilityScope.() -> Unit,
    ) {
        CompositionLocalProvider(
            LocalAnimatedVisibilityScope provides this,
            content = { content() },
        )
    }
}
