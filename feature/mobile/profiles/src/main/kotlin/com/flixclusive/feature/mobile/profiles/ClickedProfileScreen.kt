package com.flixclusive.feature.mobile.profiles

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.database.entity.user.User
import com.flixclusive.core.presentation.common.components.GradientCircularProgressIndicator
import com.flixclusive.core.presentation.common.extensions.noIndicationClickable
import com.flixclusive.core.presentation.common.util.SharedTransitionUtil.ProvideAnimatedVisibilityScope
import com.flixclusive.core.presentation.common.util.SharedTransitionUtil.ProvideSharedTransitionScope
import com.flixclusive.core.presentation.common.util.SharedTransitionUtil.getLocalAnimatedVisibilityScope
import com.flixclusive.core.presentation.common.util.SharedTransitionUtil.getLocalSharedTransitionScope
import com.flixclusive.core.presentation.mobile.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.core.presentation.mobile.components.UserAvatar
import com.flixclusive.core.presentation.mobile.components.UserAvatarDefaults.DefaultAvatarSize
import com.flixclusive.core.presentation.mobile.components.getUserBackgroundPalette
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.AdaptiveSizeUtil.getAdaptiveDp
import com.flixclusive.core.strings.R as LocaleR

@Composable
private fun getAvatarSize(): Dp {
    return getAdaptiveDp(
        dp = (DefaultAvatarSize.value * 1.5).dp,
        increaseBy = 100.dp
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun ClickedProfileScreen(
    user: User,
    isLoading: Boolean,
    onConfirm: () -> Unit,
    onBack: () -> Unit
) {
    val sharedTransitionScope = getLocalSharedTransitionScope()
    val animatedVisibilityScope = getLocalAnimatedVisibilityScope()

    BackHandler(
        enabled = !isLoading,
        onBack = onBack
    )

    val surface = MaterialTheme.colorScheme.surface
    val palette = getUserBackgroundPalette(avatar = user.image)

    val dominantSwatch = palette.dominantSwatch
    val lightVibrantSwatch = palette.darkVibrantSwatch
    val defaultColor = MaterialTheme.colorScheme.primary

    val backgroundColor = Color(dominantSwatch?.rgb ?: defaultColor.toArgb())
    val tubeLightColor = Color(lightVibrantSwatch?.rgb ?: defaultColor.toArgb())

    val brush = Brush.verticalGradient(
        listOf(
            tubeLightColor,
            backgroundColor,
            surface
        )
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .noIndicationClickable {
                    if (!isLoading) {
                        onBack()
                    }
                }
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .background(brush),
        ) {
            with(sharedTransitionScope) {
                UserAvatar(
                    avatar = user.image,
                    shadowBlur = 30.dp,
                    modifier = Modifier
                        .size(getAvatarSize())
                        .sharedElement(
                            state = rememberSharedContentState(key = "${user.id}-pager"),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                        .sharedElement(
                            state = rememberSharedContentState(key = "${user.id}-grid"),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                        .noIndicationClickable {  }
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.Center)
        ) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(
                        getAdaptiveDp(
                            dp = DefaultAvatarSize.times(2.5F),
                            increaseBy = 150.dp
                        )
                    )
            )

            AnimatedContent(
                label = "ContinueAndLoad",
                targetState = isLoading
            ) { state ->
                if (state) {
                    GradientCircularProgressIndicator(
                        size = getAdaptiveDp(40.dp, 60.dp, 60.dp),
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary,
                        )
                    )
                } else {
                    ContinueButton(onClick = onConfirm)
                }
            }
        }
    }
}

@Composable
private fun ContinueButton(
    onClick: () -> Unit
) {
    val isPressed = remember { mutableStateOf(false) }

    val buttonShape = MaterialTheme.shapes.extraSmall
    val buttonContainerGradient = with(MaterialTheme.colorScheme) {
        Brush.linearGradient(
            0F to surfaceColorAtElevation(1.dp),
            0.5F to surfaceColorAtElevation(5.dp),
            1F to surfaceColorAtElevation(10.dp),
        )
    }
    val buttonBorderGradient = with(MaterialTheme.colorScheme) {
        Brush.linearGradient(
            0F to surfaceColorAtElevation(10.dp),
            1F to onSurface.copy(0.4F),
        )
    }

    val buttonScale by animateFloatAsState(
        label = "ButtonScale",
        targetValue = if (isPressed.value) 0.9F else 1F,
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .height(getAdaptiveDp(45.dp, 16.dp))
            .width(getAvatarSize())
            .graphicsLayer {
                scaleX = buttonScale
                scaleY = buttonScale
            }
            .background(
                shape = buttonShape,
                brush = buttonContainerGradient
            )
            .border(
                width = 0.5.dp,
                brush = buttonBorderGradient,
                shape = buttonShape
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed.value = true
                        tryAwaitRelease()
                        onClick()
                        isPressed.value = false
                    }
                )
            }
    ) {
        Text(
            text = stringResource(LocaleR.string.continue_label),
            color = MaterialTheme.colorScheme.onSurface.copy(0.9F),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelLarge.asAdaptiveTextStyle(
                compact = 14.sp,
                medium = 20.sp
            )
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Preview
@Composable
private fun ClickedProfileScreenBasePreview(avatar: Int = 1) {
    FlixclusiveTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
        ) {
            ProvideSharedTransitionScope {
                AnimatedVisibility(true) {
                    ProvideAnimatedVisibilityScope {
                        ClickedProfileScreen(
                            user = remember { User(id = 1, name = "User", image = avatar) },
                            isLoading = remember { mutableStateOf(false) }.value,
                            onConfirm = {},
                            onBack = {}
                        )
                    }
                }
            }
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun ClickedProfileScreenCompactLandscapePreview() {
    ClickedProfileScreenBasePreview(avatar = 1)
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun ClickedProfileScreenMediumPortraitPreview() {
    ClickedProfileScreenBasePreview(avatar = 2)
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun ClickedProfileScreenMediumLandscapePreview() {
    ClickedProfileScreenBasePreview(avatar = 3)
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun ClickedProfileScreenExtendedPortraitPreview() {
    ClickedProfileScreenBasePreview(avatar = 4)
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun ClickedProfileScreenExtendedLandscapePreview() {
    ClickedProfileScreenBasePreview(avatar = 5)
}
