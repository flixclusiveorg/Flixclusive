package com.flixclusive.tv

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.flixclusive.AppNavigationItem
import com.flixclusive.core.ui.common.R
import com.flixclusive.core.ui.common.util.ifElse
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.ui.tv.util.LocalLastFocusedItemFocusedRequesterProvider
import com.flixclusive.core.ui.tv.util.focusOnInitialVisibility
import com.flixclusive.core.ui.tv.util.useLocalLastFocusedItemFocusedRequester
import com.ramcosta.composedestinations.spec.NavGraphSpec
import kotlinx.coroutines.delay
import com.flixclusive.core.locale.R as LocaleR

internal val tvNavigationItems = listOf(
    AppNavigationItem(
        screen = TvNavGraphs.home,
        iconSelected = R.drawable.home,
        iconUnselected = R.drawable.home_outlined,
        label = LocaleR.string.home
    ),
    AppNavigationItem(
        screen = TvNavGraphs.search,
        iconSelected = R.drawable.search,
        iconUnselected = R.drawable.search_outlined,
        label = LocaleR.string.search
    ),
)

internal val NavItemsFocusRequesters = List(size = tvNavigationItems.size) { FocusRequester() }
internal val InitialDrawerWidth = 50.dp

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun NavDrawer(
    modifier: Modifier = Modifier,
    isNavDrawerVisible: Boolean,
    currentScreen: NavGraphSpec,
    isDrawerOpen: Boolean,
    focusRequesters: List<FocusRequester> = remember { NavItemsFocusRequesters },
    onDrawerStateChange: (Boolean) -> Unit,
    onNavigate: (NavGraphSpec) -> Unit,
    content: @Composable () -> Unit
) {
    val focusManager = LocalFocusManager.current

    val drawerPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
    val expandedDrawerWidth = LocalConfiguration.current.screenWidthDp.times(0.85F).dp

    val drawerWidth by animateDpAsState(
        animationSpec = keyframes {
            delayMillis = 100
        },
        targetValue = if(isDrawerOpen) expandedDrawerWidth else InitialDrawerWidth,
        label = ""
    )
    val drawerColor = MaterialTheme.colorScheme.surface
    val isNavItemFocusedAlready = remember { mutableStateOf(false) }

    LaunchedEffect(isDrawerOpen) {
        if (!isDrawerOpen) {
            delay(500) // Add delay for UX
            isNavItemFocusedAlready.value = false
        }
    }

    LocalLastFocusedItemFocusedRequesterProvider {
        val lastItemFocusedFocusRequester = useLocalLastFocusedItemFocusedRequester()

        Box(
            modifier = modifier
        ) {
            content()

            AnimatedVisibility(
                visible = isNavDrawerVisible,
                enter = slideInHorizontally(),
                exit = slideOutHorizontally()
            ) {
                Column(
                    modifier = Modifier
                        .focusGroup()
                        .fillMaxHeight()
                        .width(drawerWidth)
                        .drawBehind {
                            drawRect(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        drawerColor,
                                        Color.Transparent
                                    ),
                                    startX = size.width.times(0.15F)
                                )
                            )
                        }
                        .padding(drawerPadding)
                        .onFocusChanged {
                            onDrawerStateChange(it.isFocused)
                        },
                    verticalArrangement = Arrangement.Center
                ) {
                    tvNavigationItems.forEachIndexed { i, item ->
                        NavDrawerItem(
                            isDrawerOpen = isDrawerOpen,
                            appDestination = item,
                            currentScreen = currentScreen,
                            onClick = {
                                onNavigate(item.screen)
                                focusManager.moveFocus(FocusDirection.Right)
                            },
                            modifier = Modifier
                                .focusRequester(focusRequesters[i])
                                .ifElse(
                                    condition = i == 0 && isDrawerOpen,
                                    ifTrueModifier = Modifier.focusOnInitialVisibility(isNavItemFocusedAlready)
                                )
                                .focusProperties {
                                    right = lastItemFocusedFocusRequester.value
                                }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun NavDrawerItem(
    modifier: Modifier = Modifier,
    isDrawerOpen: Boolean,
    appDestination: AppNavigationItem,
    currentScreen: NavGraphSpec,
    onClick: () -> Unit,
) {
    val isDestinationSelected = remember(currentScreen) {
        currentScreen == appDestination.screen
        || appDestination.screen.route.contains(
            other = currentScreen.route,
            ignoreCase = true
        )
    }
    val icon = remember(isDestinationSelected) {
        if(isDestinationSelected) {
            appDestination.iconSelected
        } else appDestination.iconUnselected
    }

    val horizontalPadding by animateDpAsState(
        animationSpec = keyframes {
            this.delayMillis = 100
        },
        targetValue = if (isDrawerOpen) 25.dp else 5.dp,
        label = "",
    )

    val verticalPadding by animateDpAsState(
        animationSpec = keyframes {
            this.delayMillis = 100
        },
        targetValue = if (isDrawerOpen) 18.dp else 12.dp,
        label = "",
    )

    Surface(
        onClick = { onClick() },
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = LocalContentColor.current.onMediumEmphasis(),
            focusedContainerColor = Color.Transparent,
            focusedContentColor = Color.White
        ),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(
                vertical = verticalPadding,
                horizontal = horizontalPadding
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = stringResource(id = appDestination.label),
                modifier = Modifier
                    .height(15.dp)
                    .aspectRatio(1F)
            )

            AnimatedVisibility(
                visible = isDrawerOpen,
                enter = slideInHorizontally() + fadeIn(),
                exit = fadeOut()
            ) {
                Text(
                    text = stringResource(id = appDestination.label),
                    maxLines = 1,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}