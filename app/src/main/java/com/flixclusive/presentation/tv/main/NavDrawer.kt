package com.flixclusive.presentation.tv.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
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
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.flixclusive.presentation.destinations.Destination
import com.flixclusive.presentation.tv.utils.ComposeTvUtils.colorOnMediumEmphasisTv
import com.flixclusive.presentation.tv.utils.ModifierTvUtils
import com.flixclusive.presentation.utils.ModifierUtils.ifElse
import com.ramcosta.composedestinations.spec.Direction

val NavItemsFocusRequesters = List(size = TvAppDestination.values().size) { FocusRequester() }
val InitialDrawerWidth = 50.dp

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalFoundationApi::class,
    ExperimentalComposeUiApi::class
)
@Composable
fun NavDrawer(
    modifier: Modifier = Modifier,
    isNavDrawerVisible: Boolean,
    currentScreen: Destination,
    isDrawerOpen: Boolean,
    focusRequesters: List<FocusRequester> = remember { NavItemsFocusRequesters },
    onDrawerStateChange: (Boolean) -> Unit,
    onNavigate: (Direction) -> Unit,
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
    val focusRestorerModifiers = ModifierTvUtils.createInitialFocusRestorerModifiers()

    val contentFocusRequester = remember { FocusRequester() }
    Box(
        modifier = modifier
    ) {
        Box(
            Modifier.focusRequester(contentFocusRequester)
        ) {
            content()
        }

        AnimatedVisibility(
            visible = isNavDrawerVisible,
            enter = slideInHorizontally(),
            exit = slideOutHorizontally()
        ) {
            Column(
                modifier = focusRestorerModifiers.parentModifier
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
                TvAppDestination.values().forEachIndexed { i, item ->
                    NavDrawerItem(
                        isDrawerOpen = isDrawerOpen,
                        appDestination = item,
                        currentScreen = currentScreen,
                        onClick = {
                            onNavigate(it)
                            focusManager.moveFocus(FocusDirection.Right)
                        },
                        modifier = Modifier
                            .focusRequester(focusRequesters[i])
                            .ifElse(
                                condition = i == 0,
                                ifTrueModifier = focusRestorerModifiers.childModifier
                            )
                            .focusProperties {
                                right = contentFocusRequester
                            }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun NavDrawerItem(
    modifier: Modifier = Modifier,
    isDrawerOpen: Boolean,
    appDestination: TvAppDestination,
    currentScreen: Destination,
    onClick: (Direction) -> Unit,
) {
    val isDestinationSelected = remember(currentScreen) {
        currentScreen == appDestination.direction
        || appDestination.direction.route.contains(
            other = currentScreen.baseRoute,
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
        onClick = { onClick(appDestination.direction) },
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = colorOnMediumEmphasisTv(),
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
                enter = slideInHorizontally { -1500 } + fadeIn(),
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