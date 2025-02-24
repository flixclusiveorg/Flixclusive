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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.GradientCircularProgressIndicator
import com.flixclusive.core.ui.common.user.UserAvatar
import com.flixclusive.core.ui.common.user.UserAvatarDefaults.DefaultAvatarSize
import com.flixclusive.core.ui.common.user.getUserBackgroundPalette
import com.flixclusive.core.ui.common.util.DummyDataForPreview.getDummyUser
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveStylesUtil.getAdaptiveTextStyle
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveUiUtil.getAdaptiveDp
import com.flixclusive.core.ui.common.util.adaptive.TextStyleMode
import com.flixclusive.core.ui.common.util.adaptive.TypographyStyle
import com.flixclusive.core.ui.common.util.animation.AnimationUtil.ProvideAnimatedVisibilityScope
import com.flixclusive.core.ui.common.util.animation.AnimationUtil.ProvideSharedTransitionScope
import com.flixclusive.core.ui.common.util.animation.AnimationUtil.getLocalAnimatedVisibilityScope
import com.flixclusive.core.ui.common.util.animation.AnimationUtil.getLocalSharedTransitionScope
import com.flixclusive.core.ui.common.util.noIndicationClickable
import com.flixclusive.model.database.User
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds
import com.flixclusive.core.locale.R as LocaleR

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
    clickedProfile: User,
    isLoading: MutableState<Boolean>,
    onConfirm: () -> Unit,
    onBack: () -> Unit
) {
    val sharedTransitionScope = getLocalSharedTransitionScope()
    val animatedVisibilityScope = getLocalAnimatedVisibilityScope()

    BackHandler(
        enabled = !isLoading.value,
        onBack = onBack
    )

    val surface = MaterialTheme.colorScheme.surface
    val palette = getUserBackgroundPalette(user = clickedProfile)

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
                    if (!isLoading.value) {
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
                    user = clickedProfile,
                    shadowBlur = 30.dp,
                    modifier = Modifier
                        .size(getAvatarSize())
                        .sharedElement(
                            state = rememberSharedContentState(key = "${clickedProfile.id}-pager"),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                        .sharedElement(
                            state = rememberSharedContentState(key = "${clickedProfile.id}-grid"),
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
                targetState = isLoading.value
            ) { isLoadingState ->
                if (isLoadingState) {
                    LaunchedEffect(true) {
                        delay(2.seconds)
                        onConfirm()
                    }

                    GradientCircularProgressIndicator(
                        size = getAdaptiveDp(
                            40.dp, 60.dp, 60.dp
                        ),
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary,
                        )
                    )
                } else {
                    ContinueButton(
                        onClick = { isLoading.value = true }
                    )
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
            style = getAdaptiveTextStyle(
                medium = 20.sp,
                style = TypographyStyle.Label,
                mode = TextStyleMode.Emphasized,
            ).copy(
                color = MaterialTheme.colorScheme.onSurface.copy(0.9F)
            )
        )
    }
}

@Preview
@Composable
private fun ClickedProfileScreenBasePreview(user: User = getDummyUser()) {
    FlixclusiveTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
        ) {
            ProvideSharedTransitionScope {
                AnimatedVisibility(true) {
                    ProvideAnimatedVisibilityScope {
                        ClickedProfileScreen(
                            clickedProfile = user,
                            isLoading = remember { mutableStateOf(false) },
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
    ClickedProfileScreenBasePreview(getDummyUser(image = 1))
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun ClickedProfileScreenMediumPortraitPreview() {
    ClickedProfileScreenBasePreview(getDummyUser(image = 2))
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun ClickedProfileScreenMediumLandscapePreview() {
    ClickedProfileScreenBasePreview(getDummyUser(image = 3))
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun ClickedProfileScreenExtendedPortraitPreview() {
    ClickedProfileScreenBasePreview(getDummyUser(image = 4))
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun ClickedProfileScreenExtendedLandscapePreview() {
    ClickedProfileScreenBasePreview(getDummyUser(image = 5))
}
