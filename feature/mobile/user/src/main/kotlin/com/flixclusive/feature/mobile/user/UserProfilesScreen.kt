package com.flixclusive.feature.mobile.user

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.CommonTopBar
import com.flixclusive.core.ui.common.navigation.navargs.UserProfilesNavArgs
import com.flixclusive.core.ui.common.navigation.navigator.UserProfilesNavigator
import com.flixclusive.core.ui.common.user.AVATARS_IMAGE_COUNT
import com.flixclusive.core.ui.mobile.util.ComposeUtil.DefaultScreenPaddingHorizontal
import com.flixclusive.feature.mobile.user.util.UxUtil.getSlidingTransition
import com.flixclusive.model.database.User
import com.ramcosta.composedestinations.annotation.Destination
import kotlinx.coroutines.launch
import com.flixclusive.core.locale.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR

@Destination(
    navArgsDelegate = UserProfilesNavArgs::class
)
@Composable
internal fun UserProfilesScreen(
    navigator: UserProfilesNavigator,
    args: UserProfilesNavArgs
) {
    val list = remember {
        List(15) {
            User(
                id = it,
                image = it % AVATARS_IMAGE_COUNT,
                name = "User $it"
            )
        }
    }

    val viewMode = rememberSaveable { mutableStateOf(ViewMode.Pager) }

    Scaffold(
        topBar = {
            TopBar(
                areProfilesEmpty = list.isEmpty(),
                viewMode = viewMode,
                isComingFromSplashScreen = args.isComingFromSplashScreen,
                addNewUser = navigator::openAddUsersScreen,
                onBack = navigator::goBack
            )
        }
    ) {
        if (list.isNotEmpty()) {
            AnimatedContent(
                label = "main_content",
                targetState = viewMode.value,
                transitionSpec = {
                    val enterDuration = 500
                    val exitDuration = 300
                    val enterTweenFloat = tween<Float>(durationMillis = enterDuration)
                    val enterTweenInt = tween<IntOffset>(durationMillis = enterDuration)
                    val exitTweenFloat = tween<Float>(durationMillis = exitDuration)
                    val exitTweenInt = tween<IntOffset>(durationMillis = exitDuration)

                    if (targetState == ViewMode.Grid) {
                        slideInHorizontally(enterTweenInt) + fadeIn(enterTweenFloat) togetherWith
                                scaleOut(exitTweenFloat) + fadeOut(exitTweenFloat)
                    } else {
                        fadeIn(enterTweenFloat) + scaleIn(exitTweenFloat) togetherWith
                                slideOutHorizontally(exitTweenInt) + fadeOut(exitTweenFloat)
                    }
                },
                modifier = Modifier.padding(it)
            ) { state ->
                when (state) {
                    ViewMode.Grid -> {
                        GridMode(
                            profiles = list,
                            onSelect = { /*TODO: Navigate to home/settings screen*/ },
                            onEdit = { /*TODO: Navigate to edit screen*/ },
                            modifier = Modifier
                                .padding(it)
                        )
                    }
                    ViewMode.Pager -> {
                        PagerMode(
                            profiles = list,
                            onSelect = { /*TODO: Navigate to home/settings screen*/ },
                            onEdit = { /*TODO: Navigate to edit screen*/ },
                            modifier = Modifier
                                .padding(it)
                        )
                    }
                }
            }
        } else {
            EmptyScreen(
                onAdd = { navigator.openAddUsersScreen() },
                modifier = Modifier.padding(it)
            )
        }
    }
}

private enum class ViewMode {
    Grid,
    Pager;
}

internal val TopBarHeight = 50.dp

@Composable
private fun TopBar(
    isComingFromSplashScreen: Boolean,
    viewMode: MutableState<ViewMode>,
    areProfilesEmpty: Boolean,
    onBack: () -> Unit,
    addNewUser: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(TopBarHeight)
            .statusBarsPadding()
    ) {

        if (areProfilesEmpty) {
            val gradientColors = listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.tertiary,
            )

            Box(
                modifier = Modifier.weight(1F),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(UiCommonR.drawable.flixclusive_tag),
                    contentDescription = stringResource(id = com.flixclusive.core.locale.R.string.flixclusive_tag_content_desc),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .height(30.dp)
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
        } else {
            TopBarForNonEmptyScreen(
                modifier = Modifier.weight(1F),
                isComingFromSplashScreen = isComingFromSplashScreen,
                onBack = onBack
            ) {
                AnimatedContent(
                    label = "view_mode_icon",
                    targetState = viewMode.value,
                    transitionSpec = {
                        getSlidingTransition(isSlidingRight = targetState.ordinal > initialState.ordinal)
                    }
                ) { state ->
                    val viewTypeDescription = stringResource(LocaleR.string.view_type_button_content_desc)
                    ActionButtonTooltip(description = viewTypeDescription) {
                        when (state) {
                            ViewMode.Grid -> {
                                IconButton(onClick = { viewMode.value = ViewMode.Pager }) {
                                    Icon(
                                        painter = painterResource(UiCommonR.drawable.view_array),
                                        contentDescription = viewTypeDescription
                                    )
                                }
                            }
                            ViewMode.Pager -> {
                                IconButton(onClick = { viewMode.value = ViewMode.Grid }) {
                                    Icon(
                                        painter = painterResource(UiCommonR.drawable.view_grid),
                                        contentDescription = viewTypeDescription
                                    )
                                }
                            }
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
                            contentDescription = addUserButton
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.TopBarForNonEmptyScreen(
    modifier: Modifier = Modifier,
    isComingFromSplashScreen: Boolean,
    onBack: () -> Unit,
    endContent: @Composable () -> Unit
) {
    val headerLabel = stringResource(id = LocaleR.string.profiles)

    if (isComingFromSplashScreen) {
        CommonTopBar(
            modifier = modifier,
            headerTitle = headerLabel,
            onNavigationIconClick = onBack,
            endContent = endContent
        )
    } else {
        Spacer(Modifier.width(DefaultScreenPaddingHorizontal))

        Box(
            modifier = Modifier.weight(1F)
        ) {
            Text(
                text = headerLabel,
                style = MaterialTheme.typography.headlineMedium,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )
        }

        endContent()


        Spacer(Modifier.width(DefaultScreenPaddingHorizontal))
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

@Preview(device = "id:Realme 5 ANDROID 10 [29]")
@Composable
private fun UserProfilesScreenPreview() {
    FlixclusiveTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
        ) {
            UserProfilesScreen(
                navigator = object : UserProfilesNavigator {
                    override fun goBack() = Unit
                    override fun onExitApplication() = Unit
                    override fun openAddUsersScreen() = Unit
                    override fun openEditUserScreen() = Unit
                    override fun openHomeScreen() = Unit
                },
                args = UserProfilesNavArgs(false)
            )
        }
    }
}