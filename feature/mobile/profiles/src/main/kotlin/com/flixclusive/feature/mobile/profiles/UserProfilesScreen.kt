package com.flixclusive.feature.mobile.profiles

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.database.entity.user.User
import com.flixclusive.core.navigation.navargs.PinVerificationResult
import com.flixclusive.core.navigation.navigator.PinAction
import com.flixclusive.core.presentation.common.extensions.showToast
import com.flixclusive.core.presentation.common.util.SharedTransitionUtil.ProvideAnimatedVisibilityScope
import com.flixclusive.core.presentation.common.util.SharedTransitionUtil.ProvideSharedTransitionScope
import com.flixclusive.core.presentation.mobile.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.core.presentation.mobile.components.provider.ProviderCrashBottomSheet
import com.flixclusive.core.presentation.mobile.components.topbar.CommonTopBar
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.AdaptiveSizeUtil.getAdaptiveDp
import com.flixclusive.feature.mobile.profiles.util.UxUtil.getSlidingTransition
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.result.NavResult
import com.ramcosta.composedestinations.result.OpenResultRecipient
import kotlinx.coroutines.launch
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

@OptIn(ExperimentalSharedTransitionApi::class)
@Destination
@Composable
internal fun UserProfilesScreen(
    navigator: UserProfilesScreenNavigator,
    isFromSplashScreen: Boolean,
    pinVerifyResultRecipient: OpenResultRecipient<PinVerificationResult>,
    viewModel: UserProfilesViewModel = hiltViewModel(),
) {
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isLoggedIn, uiState.errors) {
        if (uiState.isLoggedIn && uiState.errors.isEmpty()) {
            navigator.openHomeScreen()
        }
    }

    pinVerifyResultRecipient.onNavResult { result ->
        if (result is NavResult.Value && result.value.isVerified && uiState.focusedProfile != null) {
            viewModel.onUseProfile(uiState.focusedProfile!!)
        }
    }

    UserProfilesScreenContent(
        profiles = profiles,
        uiState = uiState,
        isFromSplashScreen = isFromSplashScreen,
        navigator = navigator,
        initialState = if (isFromSplashScreen) {
            ScreenType.ContinueScreen
        } else {
            ScreenType.Pager
        },
        onHoverProfile = viewModel::onHoverProfile,
        onUseProfile = viewModel::onUseProfile,
        onConsumeErrors = viewModel::onConsumeErrors,
    )
}

private enum class ScreenType {
    Grid,
    ContinueScreen,
    Pager,
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun UserProfilesScreenContent(
    profiles: List<User>,
    uiState: ProfilesScreenUiState,
    isFromSplashScreen: Boolean,
    navigator: UserProfilesScreenNavigator,
    initialState: ScreenType,
    onHoverProfile: (User) -> Unit,
    onUseProfile: (User) -> Unit,
    onConsumeErrors: () -> Unit,
) {
    val context = LocalContext.current

    val (pageCount, initialPage) = remember(profiles.size) {
        val pageCount = if (profiles.size <= 2) {
            profiles.size
        } else {
            Int.MAX_VALUE
        }

        val initialPage = if (profiles.size <= 2) {
            0
        } else {
            (Int.MAX_VALUE / 2) - ((Int.MAX_VALUE / 2) % profiles.size)
        }

        return@remember pageCount to initialPage
    }

    val listState = rememberLazyGridState()
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { pageCount })

    var screenType by rememberSaveable { mutableStateOf(ScreenType.Pager) }
    var lastScreenTypeUsed by rememberSaveable { mutableStateOf(ScreenType.Pager) }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .align(Alignment.Center),
        ) {
            if (profiles.isNotEmpty()) {
                ProvideSharedTransitionScope {
                    AnimatedContent(
                        label = "main_content",
                        targetState = screenType,
                        transitionSpec = {
                            val enterDuration = 500
                            val exitDuration = 300
                            val enterTweenFloat = tween<Float>(durationMillis = enterDuration)
                            val enterTweenInt = tween<IntOffset>(durationMillis = enterDuration)
                            val exitTweenFloat = tween<Float>(durationMillis = exitDuration)
                            val exitTweenInt = tween<IntOffset>(durationMillis = exitDuration)

                            val isNotSelecting = initialState != ScreenType.ContinueScreen
                            if (isNotSelecting && targetState == ScreenType.Grid) {
                                slideInHorizontally(enterTweenInt) + fadeIn(enterTweenFloat) togetherWith
                                    scaleOut(exitTweenFloat) + fadeOut(exitTweenFloat)
                            } else if (isNotSelecting && targetState == ScreenType.Pager) {
                                fadeIn(enterTweenFloat) + scaleIn(exitTweenFloat) togetherWith
                                    slideOutHorizontally(exitTweenInt) + fadeOut(exitTweenFloat)
                            } else {
                                fadeIn() togetherWith fadeOut()
                            }
                        },
                    ) { state ->
                        ProvideAnimatedVisibilityScope {
                            val onHover = fun(user: User) {
                                lastScreenTypeUsed = screenType
                                screenType = ScreenType.ContinueScreen
                                onHoverProfile(user)
                            }

                            when (state) {
                                ScreenType.Grid -> {
                                    GridMode(
                                        profiles = profiles,
                                        onHover = onHover,
                                        listState = listState,
                                        onEdit = { navigator.openEditUserScreen(it.id) },
                                    )
                                }

                                ScreenType.Pager -> {
                                    PagerMode(
                                        profiles = profiles,
                                        onHover = onHover,
                                        pagerState = pagerState,
                                        onEdit = { navigator.openEditUserScreen(it.id) },
                                    )
                                }

                                ScreenType.ContinueScreen -> {
                                    uiState.focusedProfile?.let { profile ->
                                        ClickedProfileScreen(
                                            user = profile,
                                            isLoading = uiState.isLoading,
                                            onBack = { screenType = lastScreenTypeUsed },
                                            onConfirm = {
                                                if (profile.pin.isNullOrEmpty()) {
                                                    onUseProfile(profile)
                                                } else {
                                                    navigator.openUserPinScreen(PinAction.Verify(profile.pin))
                                                }
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                EmptyScreen(
                    onAdd = { navigator.openAddProfileScreen() },
                )
            }
        }

        AnimatedVisibility(
            visible = !uiState.isLoading,
            enter = EnterTransition.None,
            exit = slideOutVertically(tween(500)) + fadeOut(),
        ) {
            TopBar(
                showTagOnly = profiles.isEmpty() || screenType == ScreenType.ContinueScreen,
                screenType = screenType,
                isFromSplashScreen = isFromSplashScreen,
                addNewUser = navigator::openAddProfileScreen,
                onChangeView = {
                    lastScreenTypeUsed = screenType
                    screenType = it
                },
                onBack = {
                    if (screenType == ScreenType.ContinueScreen) {
                        screenType = lastScreenTypeUsed
                    } else {
                        navigator.goBack()
                    }
                },
            )
        }
    }

    if (uiState.errors.isNotEmpty()) {
        val listOfErrors by remember {
            derivedStateOf {
                uiState.errors.values.toList()
            }
        }

        ProviderCrashBottomSheet(
            isLoading = uiState.isLoading,
            errors = listOfErrors,
            onDismissRequest = {
                if (uiState.isLoading) {
                    context.showToast(context.getString(R.string.sheet_dismiss_disabled_on_provider_loading))
                    return@ProviderCrashBottomSheet
                }

                onConsumeErrors()
            },
        )
    }
}

@Suppress("ktlint:compose:mutable-state-param-check")
@Composable
private fun TopBar(
    isFromSplashScreen: Boolean,
    screenType: ScreenType,
    showTagOnly: Boolean,
    onChangeView: (ScreenType) -> Unit,
    onBack: () -> Unit,
    addNewUser: () -> Unit,
) {
    val defaultTopBarHeight = getAdaptiveDp(60.dp, 8.dp)

    AnimatedContent(
        targetState = showTagOnly,
        label = "TopBar",
        transitionSpec = { fadeIn() togetherWith fadeOut() },
    ) { state ->
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.heightIn(defaultTopBarHeight),
        ) {
            if (state) {
                val gradientColors =
                    listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.tertiary,
                    )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(defaultTopBarHeight * 1.3F)
                        .background(MaterialTheme.colorScheme.surface.copy(0.4F))
                        .statusBarsPadding()
                        .padding(horizontal = getAdaptiveDp(20.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!isFromSplashScreen || screenType == ScreenType.ContinueScreen) {
                        BackButton(
                            modifier = Modifier.align(Alignment.CenterStart),
                            onBack = onBack,
                        )
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(bottom = getAdaptiveDp(10.dp, 6.dp)),
                    ) {
                        Image(
                            painter = painterResource(UiCommonR.drawable.flixclusive_tag),
                            contentDescription = stringResource(id = LocaleR.string.flixclusive_tag_content_desc),
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth(0.4F)
                                .graphicsLayer(alpha = 0.99F)
                                .drawWithCache {
                                    onDrawWithContent {
                                        drawContent()
                                        drawRect(
                                            brush = Brush.linearGradient(colors = gradientColors),
                                            blendMode = BlendMode.SrcAtop,
                                        )
                                    }
                                },
                        )
                    }
                }
            } else {
                val iconSize = getAdaptiveDp(24.dp, 10.dp)
                val sizeModifier = Modifier.size(iconSize)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(getAdaptiveDp(0.dp, 20.dp)),
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(
                            start = getAdaptiveDp(10.dp),
                            end = getAdaptiveDp(0.dp, 10.dp),
                        ),
                ) {
                    TopBarForNonEmptyScreen(
                        modifier = Modifier.weight(1F),
                        isFromSplashScreen = isFromSplashScreen,
                        onBack = onBack,
                    ) {
                        AnimatedContent(
                            label = "view_mode_icon",
                            targetState = screenType,
                            transitionSpec = {
                                getSlidingTransition(isSlidingRight = targetState.ordinal > initialState.ordinal)
                            },
                        ) { state ->
                            val viewTypeDescription = stringResource(R.string.change_view)
                            ActionButtonTooltip(description = viewTypeDescription) {
                                when (state) {
                                    ScreenType.Grid -> {
                                        IconButton(onClick = { onChangeView(ScreenType.Pager) }) {
                                            Icon(
                                                painter = painterResource(UiCommonR.drawable.view_array),
                                                contentDescription = viewTypeDescription,
                                                modifier = sizeModifier,
                                            )
                                        }
                                    }

                                    ScreenType.Pager -> {
                                        IconButton(onClick = { onChangeView(ScreenType.Grid) }) {
                                            Icon(
                                                painter = painterResource(UiCommonR.drawable.view_grid),
                                                contentDescription = viewTypeDescription,
                                                modifier = sizeModifier,
                                            )
                                        }
                                    }

                                    ScreenType.ContinueScreen -> Unit
                                }
                            }
                        }

                        val addUserButton = stringResource(LocaleR.string.add_profile)
                        ActionButtonTooltip(description = addUserButton) {
                            IconButton(onClick = addNewUser) {
                                Icon(
                                    painter = painterResource(UiCommonR.drawable.add_person),
                                    contentDescription = addUserButton,
                                    modifier = sizeModifier,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BackButton(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
) {
    Box(
        modifier = modifier
            .clickable(
                onClick = onBack,
                interactionSource = null,
                indication =
                    ripple(
                        radius = 30.dp,
                        bounded = false,
                    ),
            ),
    ) {
        AdaptiveIcon(
            painter = painterResource(UiCommonR.drawable.left_arrow),
            contentDescription = stringResource(LocaleR.string.navigate_up),
            tint = MaterialTheme.colorScheme.onSurface.copy(0.7F),
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Composable
private fun RowScope.TopBarForNonEmptyScreen(
    isFromSplashScreen: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    endContent: @Composable RowScope.() -> Unit,
) {
    val headerLabel = stringResource(id = LocaleR.string.profiles)

    if (!isFromSplashScreen) {
        CommonTopBar(
            modifier = modifier,
            title = headerLabel,
            onNavigate = onBack,
            actions = endContent,
        )
    } else {
        Box(
            modifier = Modifier.weight(1F),
        ) {
            Text(
                text = headerLabel,
                style = MaterialTheme.typography.headlineMedium.asAdaptiveTextStyle(24.sp, increaseBy = 8.sp),
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        }

        endContent()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionButtonTooltip(
    description: String,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val tooltipState = rememberTooltipState(isPersistent = true)
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        content = content,
        tooltip = {
            RichTooltip(
                action = {
                    TextButton(
                        onClick = {
                            scope.launch { tooltipState.dismiss() }
                        },
                    ) {
                        Text(stringResource(id = LocaleR.string.ok))
                    }
                },
            ) {
                Text(text = description)
            }
        },
        state = tooltipState,
    )
}

@Preview
@Composable
private fun UserProfilesScreenBasePreview() {
    var uiState by remember { mutableStateOf(ProfilesScreenUiState()) }

    FlixclusiveTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            UserProfilesScreenContent(
                profiles = listOf(
                    User(id = 1, name = "User 1", image = 1),
                    User(id = 2, name = "User 2", image = 2),
                    User(id = 3, name = "User 3", image = 3),
                    User(id = 4, name = "User 4", image = 4),
                ),
                uiState = uiState,
                isFromSplashScreen = false,
                navigator = object : UserProfilesScreenNavigator {
                    override fun openHomeScreen() {}

                    override fun openAddProfileScreen(isInitializing: Boolean) {}

                    override fun onExitApplication() {}

                    override fun openUserAvatarSelectScreen(selected: Int) {}

                    override fun openEditUserScreen(userId: Int) {}

                    override fun openUserPinScreen(action: PinAction) {}

                    override fun goBack() {}
                },
                initialState = ScreenType.Pager,
                onHoverProfile = { uiState = uiState.copy(focusedProfile = it) },
                onUseProfile = { uiState = uiState.copy(isLoading = true) },
                onConsumeErrors = { uiState = uiState.copy(errors = emptyMap()) },
            )
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun UserProfilesScreenCompactLandscapePreview() {
    UserProfilesScreenBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun UserProfilesScreenMediumPortraitPreview() {
    UserProfilesScreenBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun UserProfilesScreenMediumLandscapePreview() {
    UserProfilesScreenBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun UserProfilesScreenExtendedPortraitPreview() {
    UserProfilesScreenBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun UserProfilesScreenExtendedLandscapePreview() {
    UserProfilesScreenBasePreview()
}
