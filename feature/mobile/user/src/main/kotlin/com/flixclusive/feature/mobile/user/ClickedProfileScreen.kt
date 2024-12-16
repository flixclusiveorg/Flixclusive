package com.flixclusive.feature.mobile.user

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.GradientCircularProgressIndicator
import com.flixclusive.core.ui.common.user.DefaultAvatarSize
import com.flixclusive.core.ui.common.user.UserAvatar
import com.flixclusive.core.ui.common.user.getAdaptiveBackground
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveStylesUtil.getAdaptiveEmphasizedLabel
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveUiUtil.getAdaptiveDp
import com.flixclusive.core.ui.common.util.animation.AnimationUtil.ProvideSharedTransitionScope
import com.flixclusive.core.ui.common.util.animation.AnimationUtil.getLocalAnimatedVisibilityScope
import com.flixclusive.core.ui.common.util.animation.AnimationUtil.getLocalSharedTransitionScope
import com.flixclusive.core.ui.common.util.noIndicationClickable
import com.flixclusive.model.database.User
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds
import com.flixclusive.core.locale.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR

@Composable
private fun getAvatarSize(): Dp {
    return getAdaptiveDp(
        dp = (DefaultAvatarSize.value * 1.5).dp,
        incrementedDp = 100.dp
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun ClickedProfileScreen(
    modifier: Modifier = Modifier,
    clickedProfile: User,
    onUseAsDefault: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onBack: () -> Unit
) {
    val sharedTransitionScope = getLocalSharedTransitionScope()
    val animatedVisibilityScope = getLocalAnimatedVisibilityScope()

    val isPressed = remember { mutableStateOf(false) }
    val isLoading = rememberSaveable { mutableStateOf(false) }

    val backgroundStrength by animateFloatAsState(
        label = "BackgroundStrength",
        animationSpec = tween(400),
        targetValue = if (isPressed.value) 0.2F else 0.1F,
    )

    BackHandler(
        enabled = !isLoading.value,
        onBack = onBack
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

        AnimatedVisibility(
            visible = !isLoading.value,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(getAdaptiveDp(20.dp))
        ) {
            BackButton(
                onBack = onBack
            )
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .background(
                    getAdaptiveBackground(
                        user = clickedProfile,
                        strength = backgroundStrength
                    )
                ),
        ) {
            with(sharedTransitionScope) {
                UserAvatar(
                    user = clickedProfile,
                    boxShadowBlur = 30.dp,
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
                            incrementedDp = 150.dp
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
                        isPressed = isPressed,
                        onClick = { isLoading.value = true }
                    )
                }
            }
        }
    }
}

@Composable
private fun BackButton(
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    Box(modifier = modifier) {
        Icon(
            painter = painterResource(UiCommonR.drawable.left_arrow),
            contentDescription = stringResource(LocaleR.string.navigate_up),
            tint = MaterialTheme.colorScheme.onSurface.copy(0.7F),
            modifier = Modifier
                .align(Alignment.Center)
                .size(getAdaptiveDp(20.dp, 10.dp))
        )
    }
}

@Composable
private fun ContinueButton(
    isPressed: MutableState<Boolean>,
    onClick: () -> Unit
) {
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
            style = getAdaptiveEmphasizedLabel(
                mediumFontSize = 20.sp,
            ).copy(
                MaterialTheme.colorScheme.onSurface.copy(0.9F)
            )
        )
    }
}

@Preview
@Composable
private fun ClickedProfileScreenBasePreview() {
    FlixclusiveTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
        ) {
            ProvideSharedTransitionScope {
                ClickedProfileScreen(
                    clickedProfile = User(image = 5),
                    onUseAsDefault = {},
                    onConfirm = {},
                    onBack = {}
                )
            }
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun ClickedProfileScreenCompactLandscapePreview() {
    ClickedProfileScreenBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun ClickedProfileScreenMediumPortraitPreview() {
    ClickedProfileScreenBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun ClickedProfileScreenMediumLandscapePreview() {
    ClickedProfileScreenBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun ClickedProfileScreenExtendedPortraitPreview() {
    ClickedProfileScreenBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun ClickedProfileScreenExtendedLandscapePreview() {
    ClickedProfileScreenBasePreview()
}