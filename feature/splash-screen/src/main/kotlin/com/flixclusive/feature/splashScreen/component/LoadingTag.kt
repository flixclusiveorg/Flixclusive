package com.flixclusive.feature.splashScreen.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.GradientCircularProgressIndicator
import com.flixclusive.feature.splashScreen.APP_TAG_KEY
import com.flixclusive.feature.splashScreen.ENTER_DELAY
import com.flixclusive.feature.splashScreen.EXIT_DELAY
import com.flixclusive.feature.splashScreen.TagSize
import kotlinx.coroutines.delay

@Stable
@Composable
internal fun getGradientColors()
    = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
    )

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun LoadingTag(
    isLoading: Boolean,
    animatedScope: AnimatedVisibilityScope,
    sharedTransitionScope: SharedTransitionScope
) {
    val gradientColors = getGradientColors()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically)
    ) {
        Tag(
            animatedScope = animatedScope,
            sharedTransitionScope = sharedTransitionScope
        )

        with(animatedScope) {
            if (isLoading) {
                GradientCircularProgressIndicator(
                    colors = gradientColors,
                    modifier = Modifier
                        .animateEnterExit(
                            enter = fadeIn(
                                animationSpec = tween(durationMillis = EXIT_DELAY)
                            ) + scaleIn(),
                            exit = scaleOut(
                                animationSpec = tween(durationMillis = ENTER_DELAY)
                            ) + fadeOut(),
                        )
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun Tag(
    animatedScope: AnimatedVisibilityScope,
    sharedTransitionScope: SharedTransitionScope
) {
    val gradientColors = getGradientColors()
    with(sharedTransitionScope) {
        Box(
            modifier = Modifier
                .sharedElement(
                    state = rememberSharedContentState(key = APP_TAG_KEY),
                    animatedVisibilityScope = animatedScope
                )
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Image(
                painter = painterResource(com.flixclusive.core.ui.common.R.drawable.flixclusive_tag),
                contentDescription = stringResource(id = com.flixclusive.core.locale.R.string.flixclusive_tag_content_desc),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .width(TagSize)
                    .graphicsLayer(alpha = 0.99F)
                    .drawWithCache {
                        onDrawWithContent {
                            drawContent()
                            drawRect(
                                brush = Brush.linearGradient(colors = gradientColors),
                                blendMode = BlendMode.SrcAtop
                            )
                        }
                    }
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Preview
@Composable
private fun LoadingTagPreview() {
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(isLoading) {
        delay(3000L)
        isLoading = !isLoading
    }

    FlixclusiveTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
        ) {
            SharedTransitionLayout {
                AnimatedContent(
                    isLoading,
                    label = "test_transition"
                ) {
                    LoadingTag(
                        isLoading = it,
                        animatedScope = this@AnimatedContent,
                        sharedTransitionScope = this@SharedTransitionLayout
                    )
                }
            }
        }
    }
}