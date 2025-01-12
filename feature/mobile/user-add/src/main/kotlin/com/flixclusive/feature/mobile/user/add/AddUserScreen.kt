package com.flixclusive.feature.mobile.user.add

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.imageLoader
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.navigation.navargs.PinWithHintResult
import com.flixclusive.core.ui.common.navigation.navigator.GoBackAction
import com.flixclusive.core.ui.common.navigation.navigator.SelectAvatarAction
import com.flixclusive.core.ui.common.navigation.navigator.SetupPinAction
import com.flixclusive.core.ui.common.navigation.navigator.StartHomeScreenAction
import com.flixclusive.core.ui.common.util.CoilUtil.ProvideAsyncImagePreviewHandler
import com.flixclusive.core.ui.common.util.CoilUtil.buildImageUrl
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveStylesUtil.getAdaptiveTextStyle
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveUiUtil.getAdaptiveDp
import com.flixclusive.core.ui.common.util.adaptive.TextStyleMode
import com.flixclusive.core.ui.common.util.adaptive.TypographyStyle
import com.flixclusive.core.ui.common.util.noIndicationClickable
import com.flixclusive.feature.mobile.user.add.component.AddUserScaffold
import com.flixclusive.feature.mobile.user.add.component.NavigationButtons
import com.flixclusive.feature.mobile.user.add.screens.AvatarScreen
import com.flixclusive.feature.mobile.user.add.screens.NameScreen
import com.flixclusive.feature.mobile.user.add.screens.PinScreen
import com.flixclusive.feature.mobile.user.add.util.ModifierUtil.fillOnBoardingContentWidth
import com.flixclusive.feature.mobile.user.add.util.ModifierUtil.getHorizontalPadding
import com.flixclusive.feature.mobile.user.add.util.StateHoistingUtil.LocalUserToAdd
import com.flixclusive.feature.mobile.user.add.util.StateHoistingUtil.ProvideUserToAdd
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.result.NavResult
import com.ramcosta.composedestinations.result.OpenResultRecipient
import kotlinx.collections.immutable.persistentListOf
import com.flixclusive.core.locale.R as LocaleR

private const val LANDSCAPE_CONTENT_WIDTH_FRACTION = 0.5F

interface AddUserScreenNavigator :
    GoBackAction,
    StartHomeScreenAction,
    SetupPinAction,
    SelectAvatarAction

@Destination
@Composable
fun AddUserScreen(
    isInitializing: Boolean,
    navigator: AddUserScreenNavigator,
    avatarResultRecipient: OpenResultRecipient<Int>,
    pinResultRecipient: OpenResultRecipient<PinWithHintResult>,
) {
    val viewModel = hiltViewModel<AddUserViewModel>()
    val context = LocalContext.current
    val orientation = LocalConfiguration.current.orientation
    val isLandscape = orientation == Configuration.ORIENTATION_LANDSCAPE
    val images by viewModel.images.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()

    var currentScreen by rememberSaveable { mutableIntStateOf(0) }
    var canSkip by rememberSaveable { mutableStateOf(false) }
    val screens =
        remember {
            persistentListOf(
                NameScreen,
                AvatarScreen(navigator),
                PinScreen(navigator),
            )
        }

    LaunchedEffect(state) {
        if (state is AddUserState.Added && isInitializing) {
            navigator.openHomeScreen()
        } else if (state is AddUserState.Added) {
            navigator.goBack()
        }
    }

    ProvideUserToAdd(user = viewModel.user) {
        val user = LocalUserToAdd.current

        pinResultRecipient.onNavResult {
            if (it is NavResult.Value) {
                val (pin, hint) = it.value
                user.value =
                    user.value.copy(
                        pin = pin,
                        pinHint = hint,
                    )
            }
        }

        avatarResultRecipient.onNavResult {
            if (it is NavResult.Value) {
                user.value =
                    user.value.copy(
                        image = it.value,
                    )
            }
        }

        AddUserScaffold(
            hideBackButton = { isInitializing && currentScreen == 0 },
            onBack = {
                if (currentScreen == 0) {
                    navigator.goBack()
                } else {
                    currentScreen--
                }
            },
        ) {
            OnBoardingBackground(
                backgroundUrl = images.getOrNull(currentScreen),
            )

            AnimatedContent(
                targetState = currentScreen,
                label = "OnBoardingContent",
                transitionSpec = {
                    val tweenInt = tween<IntOffset>(durationMillis = 300)
                    val tweenFloat = tween<Float>(durationMillis = 500)
                    val widthDivisor = 6

                    if (targetState > initialState) {
                        fadeIn(
                            tweenFloat,
                        ) + slideInHorizontally(animationSpec = tweenInt) { it / widthDivisor } togetherWith
                            fadeOut() + slideOutHorizontally { -it / widthDivisor }
                    } else {
                        fadeIn(
                            tweenFloat,
                        ) + slideInHorizontally(tweenInt) { -it / widthDivisor } + fadeIn() togetherWith
                            fadeOut() + slideOutHorizontally { it / widthDivisor }
                    }.using(
                        SizeTransform(clip = false),
                    )
                },
                modifier = Modifier.fillMaxSize(),
            ) { position ->
                val screen = screens[position]

                /*
                 * Enqueue next images
                 * */
                LaunchedEffect(true) {
                    val request =
                        context.buildImageUrl(
                            imagePath = images.getOrNull(currentScreen + 1),
                            imageSize = "original",
                        )

                    if (request != null) {
                        context.imageLoader.enqueue(request)
                    }
                }

                if (screen is NameScreen) {
                    val focusManager = LocalFocusManager.current
                    val keyboardController = LocalSoftwareKeyboardController.current

                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .noIndicationClickable {
                                    focusManager.clearFocus(true)
                                    keyboardController?.hide()
                                },
                    )
                }
            }

            Column(
                verticalArrangement =
                    Arrangement.spacedBy(
                        space = getAdaptiveDp(dp = 15.dp, increaseBy = 3.dp),
                        alignment = if (isLandscape) Alignment.CenterVertically else Alignment.Top,
                    ),
                horizontalAlignment = if (isLandscape) Alignment.End else Alignment.Start,
                modifier =
                    Modifier
                        .align(if (isLandscape) Alignment.CenterEnd else Alignment.Center)
                        .padding(
                            horizontal = getHorizontalPadding(),
                            vertical = if (!isLandscape) getHorizontalPadding() else 0.dp,
                        ),
            ) {
                val contentModifier =
                    if (isLandscape) {
                        Modifier.fillMaxWidth(LANDSCAPE_CONTENT_WIDTH_FRACTION)
                    } else {
                        Modifier.weight(1F)
                    }

                AnimatedContent(
                    targetState = currentScreen,
                    label = "OnBoardingBackground",
                    transitionSpec = {
                        val tweenInt = tween<IntOffset>(durationMillis = 300)
                        val tweenFloat = tween<Float>(durationMillis = 500)
                        val widthDivisor = 6

                        if (targetState > initialState) {
                            fadeIn(
                                tweenFloat,
                            ) + slideInHorizontally(animationSpec = tweenInt) { it / widthDivisor } togetherWith
                                fadeOut() + slideOutHorizontally { -it / widthDivisor }
                        } else {
                            fadeIn(
                                tweenFloat,
                            ) + slideInHorizontally(tweenInt) { -it / widthDivisor } + fadeIn() togetherWith
                                fadeOut() + slideOutHorizontally { it / widthDivisor }
                        }.using(
                            SizeTransform(clip = false),
                        )
                    },
                    modifier = contentModifier,
                ) { position ->
                    val screen = screens[position]

                    LaunchedEffect(true) {
                        canSkip = screen.canSkip
                    }

                    if (!isLandscape) {
                        AddUserPortraitScreen(
                            screen = screen,
                            modifier = contentModifier,
                        )
                    } else {
                        AddUserLandscapeScreen(
                            screen = screen,
                            modifier = contentModifier,
                        )
                    }
                }

                val disableNextButton =
                    when (screens[currentScreen]) {
                        is NameScreen -> user.value.name.isEmpty()
                        is PinScreen -> user.value.pin == null
                        else -> false
                    }

                NavigationButtons(
                    canSkip = canSkip,
                    disableNextButton = disableNextButton,
                    isFinalStep = currentScreen == screens.lastIndex,
                    onNext = {
                        if (currentScreen == screens.lastIndex) {
                            viewModel.addUser(
                                user = user.value,
                                isSigningIn = isInitializing,
                            )
                        } else {
                            currentScreen++
                        }
                    },
                    modifier =
                        Modifier.fillMaxWidth(
                            if (isLandscape) LANDSCAPE_CONTENT_WIDTH_FRACTION else 1F,
                        ),
                )
            }
        }
    }
}

@Composable
private fun AddUserPortraitScreen(
    screen: OnBoardingScreen,
    modifier: Modifier = Modifier,
) {
    val widthModifier = Modifier.fillOnBoardingContentWidth()

    Column(
        verticalArrangement =
            Arrangement.spacedBy(
                space = getAdaptiveDp(dp = 5.dp, increaseBy = 3.dp),
                alignment = Alignment.Bottom,
            ),
        horizontalAlignment = Alignment.Start,
        modifier = modifier,
    ) {
        Text(
            text = screen.title.asString(),
            style =
                getAdaptiveTextStyle(
                    size = 22.sp,
                    increaseBy = 16.sp,
                    style = TypographyStyle.Headline,
                    mode = TextStyleMode.Emphasized,
                ),
            modifier =
                Modifier
                    .padding(top = 20.dp),
        )

        Text(
            text = screen.description.asString(),
            style =
                getAdaptiveTextStyle(
                    size = 14.sp,
                    increaseBy = 10.sp,
                    style = TypographyStyle.Body,
                    mode = TextStyleMode.NonEmphasized,
                ),
            modifier =
                widthModifier
                    .padding(bottom = getAdaptiveDp(10.dp)),
        )

        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = widthModifier,
        ) {
            screen.Content()
        }
    }
}

@Composable
private fun AddUserLandscapeScreen(
    screen: OnBoardingScreen,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(5.dp),
        horizontalAlignment = Alignment.Start,
        modifier = modifier,
    ) {
        Text(
            text = screen.title.asString(),
            style =
                getAdaptiveTextStyle(
                    size = 24.sp,
                    increaseBy = 6.sp,
                    style = TypographyStyle.Display,
                    mode = TextStyleMode.Emphasized,
                ),
            modifier =
                Modifier
                    .padding(top = 20.dp),
        )

        Text(
            text = screen.description.asString(),
            style =
                getAdaptiveTextStyle(
                    size = 16.sp,
                    increaseBy = 6.sp,
                    style = TypographyStyle.Body,
                    mode = TextStyleMode.NonEmphasized,
                ),
            modifier =
                Modifier
                    .padding(bottom = 10.dp),
        )

        screen.Content()
    }
}

@Composable
private fun OnBoardingBackground(
    backgroundUrl: String?,
    modifier: Modifier = Modifier,
) {
    val surface = MaterialTheme.colorScheme.surface
    val orientation = LocalConfiguration.current.orientation

    Box(
        modifier =
            modifier
                .drawWithContent {
                    drawContent()
                    if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                        drawRect(
                            Brush.verticalGradient(
                                0F to surface,
                                0.2F to surface.copy(0.8F),
                                0.5F to surface.copy(0.6F),
                                0.7F to surface,
                            ),
                        )
                    } else {
                        drawRect(
                            Brush.verticalGradient(
                                0F to surface,
                                0.4F to surface.copy(0.3F),
                                1F to Color.Transparent,
                            ),
                        )
                        drawRect(
                            Brush.horizontalGradient(
                                0F to surface.copy(0.8F),
                                1F to surface,
                            ),
                        )
                    }
                },
    ) {
        ProvideAsyncImagePreviewHandler {
            AsyncImage(
                model =
                    LocalContext.current
                        .buildImageUrl(
                            imagePath = backgroundUrl,
                            imageSize = "original",
                        ),
                contentDescription = stringResource(LocaleR.string.on_boarding_background_content_desc),
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .fillMaxHeight(),
            )
        }
    }
}

@SuppressLint("ComposableNaming")
@Preview
@Composable
private fun AddUserScreenBasePreview() {
    FlixclusiveTheme {
        Surface {
            AddUserScreen(
                isInitializing = false,
                navigator =
                    object : AddUserScreenNavigator {
                        override fun openUserAvatarSelectScreen(selected: Int) = Unit

                        override fun openUserPinSetupScreen(
                            currentPin: String?,
                            isRemovingPin: Boolean,
                        ) = Unit

                        override fun goBack() = Unit

                        override fun openHomeScreen() = Unit
                    },
                avatarResultRecipient =
                    object : OpenResultRecipient<Int> {
                        @Composable
                        override fun onNavResult(listener: (NavResult<Int>) -> Unit) = Unit
                    },
                pinResultRecipient =
                    object : OpenResultRecipient<PinWithHintResult> {
                        @Composable
                        override fun onNavResult(listener: (NavResult<PinWithHintResult>) -> Unit) = Unit
                    },
            )
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun AddUserScreenCompactLandscapePreview() {
    AddUserScreenBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun AddUserScreenMediumPortraitPreview() {
    AddUserScreenBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun AddUserScreenMediumLandscapePreview() {
    AddUserScreenBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun AddUserScreenExtendedPortraitPreview() {
    AddUserScreenBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun AddUserScreenExtendedLandscapePreview() {
    AddUserScreenBasePreview()
}
