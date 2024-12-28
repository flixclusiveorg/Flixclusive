package com.flixclusive.feature.mobile.profiles

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
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
import androidx.compose.runtime.MutableState
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.CommonTopBar
import com.flixclusive.core.ui.common.navigation.GoBackAction
import com.flixclusive.core.ui.common.navigation.StartHomeScreenAction
import com.flixclusive.core.ui.common.navigation.navigator.AddProfileNavigator
import com.flixclusive.core.ui.common.navigation.navigator.ExitNavigator
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveUiUtil.getAdaptiveDp
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveUiUtil.getAdaptiveTextUnit
import com.flixclusive.core.ui.common.util.animation.AnimationUtil.ProvideAnimatedVisibilityScope
import com.flixclusive.core.ui.common.util.animation.AnimationUtil.ProvideSharedTransitionScope
import com.flixclusive.feature.mobile.profiles.util.UxUtil.getSlidingTransition
import com.flixclusive.model.database.User
import com.ramcosta.composedestinations.annotation.Destination
import kotlinx.coroutines.launch
import com.flixclusive.core.locale.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR

interface UserProfilesNavigator
    : ExitNavigator, GoBackAction, StartHomeScreenAction, AddProfileNavigator {
    fun openEditUserScreen(user: User)
}

@Destination
@Composable
internal fun UserProfilesScreen(
    navigator: UserProfilesNavigator,
    isFromSplashScreen: Boolean,
) {
    val viewModel = hiltViewModel<UserProfilesViewModel>()
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()


    val (pageCount, initialPage) = remember(profiles.size) {
        val pageCount = if (profiles.size <= 2) {
            profiles.size
        } else Int.MAX_VALUE

        val initialPage = if (profiles.size <= 2) 0
        else (Int.MAX_VALUE / 2) - ((Int.MAX_VALUE / 2) % profiles.size)

        return@remember pageCount to initialPage
    }

    val listState = rememberLazyGridState()
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { pageCount }
    )

    val screenType = rememberSaveable { mutableStateOf(ScreenType.Pager) }
    val lastScreenTypeUsed = rememberSaveable { mutableStateOf(ScreenType.Pager) }
    val isContinueScreenLoading = rememberSaveable { mutableStateOf(false) }
    var clickedProfile by remember { mutableStateOf<User?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.Center)
        ) {
            if (profiles.isNotEmpty()) {
                ProvideSharedTransitionScope {
                    AnimatedContent(
                        label = "main_content",
                        targetState = screenType.value,
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
                            }
                            else if (isNotSelecting && targetState == ScreenType.Pager) {
                                fadeIn(enterTweenFloat) + scaleIn(exitTweenFloat) togetherWith
                                        slideOutHorizontally(exitTweenInt) + fadeOut(exitTweenFloat)
                            }
                            else {
                                fadeIn() togetherWith fadeOut()
                            }
                        }
                    ) { state ->
                        ProvideAnimatedVisibilityScope {
                            val onSelect = fun (user: User) {
                                lastScreenTypeUsed.value = screenType.value
                                screenType.value = ScreenType.ContinueScreen
                                clickedProfile = user
                            }
                            when (state) {
                                ScreenType.Grid -> {
                                    GridMode(
                                        profiles = profiles,
                                        onSelect = onSelect,
                                        listState = listState,
                                        onEdit = navigator::openEditUserScreen
                                    )
                                }
                                ScreenType.Pager -> {
                                    PagerMode(
                                        profiles = profiles,
                                        onSelect = onSelect,
                                        pagerState = pagerState,
                                        onEdit = navigator::openEditUserScreen
                                    )
                                }
                                ScreenType.ContinueScreen -> {
                                    clickedProfile?.let { profile ->
                                        ClickedProfileScreen(
                                            clickedProfile = profile,
                                            isLoading = isContinueScreenLoading,
                                            onConfirm = {
                                                scope.launch {
                                                    viewModel.onUseProfile(profile)
                                                    navigator.openHomeScreen()
                                                }
                                            },
                                            onBack = {
                                                screenType.value = lastScreenTypeUsed.value
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                EmptyScreen(
                    onAdd = { navigator.openAddProfileScreen() }
                )
            }
        }

        AnimatedVisibility(
            visible = !isContinueScreenLoading.value,
            enter = EnterTransition.None,
            exit = slideOutVertically(tween(500)) + fadeOut()
        ) {
            TopBar(
                showTagOnly = profiles.isEmpty() || screenType.value == ScreenType.ContinueScreen,
                screenType = screenType,
                lastScreenTypeUsed = lastScreenTypeUsed,
                isFromSplashScreen = isFromSplashScreen,
                addNewUser = navigator::openAddProfileScreen,
                onBack = navigator::goBack
            )
        }

    }
}

private enum class ScreenType {
    Grid,
    ContinueScreen,
    Pager;
}

@Composable
private fun TopBar(
    isFromSplashScreen: Boolean,
    screenType: MutableState<ScreenType>,
    lastScreenTypeUsed: MutableState<ScreenType>,
    showTagOnly: Boolean,
    onBack: () -> Unit,
    addNewUser: () -> Unit,
) {
    val defaultTopBarHeight = getAdaptiveDp(60.dp, 15.dp)

    AnimatedContent(
        targetState = showTagOnly,
        label = "TopBar",
        transitionSpec = { fadeIn() togetherWith fadeOut() }
    ) { state ->
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .heightIn(defaultTopBarHeight)
        ) {
            if (state) {
                val gradientColors = listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.tertiary,
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(defaultTopBarHeight * 1.3F)
                        .background(
                            MaterialTheme.colorScheme.surface.copy(0.4F)
                        )
                        .statusBarsPadding()
                        .padding(horizontal = getAdaptiveDp(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!isFromSplashScreen || screenType.value == ScreenType.ContinueScreen) {
                        BackButton(
                            onBack = onBack,
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                        )
                    }

                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(UiCommonR.drawable.flixclusive_tag),
                            contentDescription = stringResource(id = com.flixclusive.core.locale.R.string.flixclusive_tag_content_desc),
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .height(getAdaptiveDp(30.dp, 20.dp))
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
                        )
                ) {
                    TopBarForNonEmptyScreen(
                        modifier = Modifier.weight(1F),
                        isFromSplashScreen = isFromSplashScreen,
                        onBack = onBack
                    ) {
                        AnimatedContent(
                            label = "view_mode_icon",
                            targetState = screenType.value,
                            transitionSpec = {
                                getSlidingTransition(isSlidingRight = targetState.ordinal > initialState.ordinal)
                            }
                        ) { state ->
                            val viewTypeDescription = stringResource(LocaleR.string.view_type_button_content_desc)
                            ActionButtonTooltip(description = viewTypeDescription) {
                                when (state) {
                                    ScreenType.Grid -> {
                                        IconButton(
                                            onClick = {
                                                lastScreenTypeUsed.value = screenType.value
                                                screenType.value = ScreenType.Pager
                                            }
                                        ) {
                                            Icon(
                                                painter = painterResource(UiCommonR.drawable.view_array),
                                                contentDescription = viewTypeDescription,
                                                modifier = sizeModifier
                                            )
                                        }
                                    }
                                    ScreenType.Pager -> {
                                        IconButton(
                                            onClick = {
                                                lastScreenTypeUsed.value = screenType.value
                                                screenType.value = ScreenType.Grid
                                            }
                                        ) {
                                            Icon(
                                                painter = painterResource(UiCommonR.drawable.view_grid),
                                                contentDescription = viewTypeDescription,
                                                modifier = sizeModifier
                                            )
                                        }
                                    }
                                    ScreenType.ContinueScreen -> Unit
                                }
                            }

                        }

                        val addUserButton = stringResource(LocaleR.string.add_user_button_content_desc)
                        ActionButtonTooltip(
                            description = addUserButton
                        ) {
                            IconButton(onClick = addNewUser) {
                                Icon(
                                    painter = painterResource(UiCommonR.drawable.add_person),
                                    contentDescription = addUserButton,
                                    modifier = sizeModifier
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
    onBack: () -> Unit
) {
    val iconSize = getAdaptiveDp(16.dp, 10.dp)

    Box(
        modifier = modifier
            .clickable(
                onClick = onBack,
                interactionSource = null,
                indication = ripple(
                    radius = iconSize / 2,
                    bounded = false,
                )
            )
    ) {
        Icon(
            painter = painterResource(UiCommonR.drawable.left_arrow),
            contentDescription = stringResource(LocaleR.string.navigate_up),
            tint = MaterialTheme.colorScheme.onSurface.copy(0.7F),
            modifier = Modifier
                .align(Alignment.Center)
                .size(iconSize)
        )
    }
}

@Composable
private fun RowScope.TopBarForNonEmptyScreen(
    modifier: Modifier = Modifier,
    isFromSplashScreen: Boolean,
    onBack: () -> Unit,
    endContent: @Composable () -> Unit
) {
    val headerLabel = stringResource(id = LocaleR.string.profiles)

    if (!isFromSplashScreen) {
        CommonTopBar(
            modifier = modifier,
            title = headerLabel,
            onNavigate = onBack,
            endContent = endContent
        )
    } else {
        Box(
            modifier = Modifier.weight(1F)
        ) {
            Text(
                text = headerLabel,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = getAdaptiveTextUnit(24.sp, increaseBy = 8)
                ),
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )
        }

        endContent()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionButtonTooltip(
    description: String,
    content: @Composable () -> Unit
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
                        }
                    ) {
                        Text(stringResource(id = LocaleR.string.ok))
                    }
                },
            ) {
                Text(text = description)
            }
        },
        state = tooltipState
    )
}

@Preview
@Composable
private fun UserProfilesScreenBasePreview() {
    FlixclusiveTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
        ) {
            UserProfilesScreen(
                isFromSplashScreen = false,
                navigator = object : UserProfilesNavigator {
                    override fun goBack() = Unit
                    override fun onExitApplication() = Unit
                    override fun openAddProfileScreen(isInitializing: Boolean) = Unit
                    override fun openEditUserScreen(user: User) = Unit
                    override fun openHomeScreen() = Unit
                }
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