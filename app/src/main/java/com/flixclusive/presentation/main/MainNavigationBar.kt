package com.flixclusive.presentation.main

import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.flixclusive.presentation.NavGraphs
import com.flixclusive.presentation.destinations.Destination
import com.ramcosta.composedestinations.navigation.navigate
import com.ramcosta.composedestinations.navigation.popUpTo
import com.ramcosta.composedestinations.spec.Direction
import com.ramcosta.composedestinations.spec.NavGraphSpec
import com.ramcosta.composedestinations.spec.Route

val NAVIGATION_BAR_HEIGHT = 100.dp

@Composable
fun MainNavigationBar(
    currentScreen: Destination,
    onNavigate: (Direction) -> Unit,
    onButtonClickTwice: (NavGraphSpec) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        modifier = Modifier
            .height(NAVIGATION_BAR_HEIGHT)
            .drawBehind {
                val strokeWidth = 2F
                val x = size.width - strokeWidth

                drawLine(
                    color = Color.LightGray,
                    start = Offset(0F, 0F),
                    end = Offset(x, 0F),
                    strokeWidth = strokeWidth
                )
            }
    ) {
        MainDestination.values().forEach {
            val icon = remember(currentScreen) {
                if(currentScreen == it.direction) {
                    it.iconSelected
                } else it.iconUnselected
            }

            NavigationBarItem(
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    indicatorColor = MaterialTheme.colorScheme.surface
                ),
                selected = currentScreen == it.direction,
                onClick = {
                    if(it.direction == currentScreen) {
                        onButtonClickTwice(it.navGraph)
                        return@NavigationBarItem
                    }

                    onNavigate(it.direction)
                },
                icon = {
                    Icon(
                        painter = icon.asPainterResource(),
                        contentDescription = stringResource(it.label)
                    )
                }
            )
        }
    }
}

fun NavHostController.navigateSingleTopTo(
    direction: Direction,
    route: Route = NavGraphs.root
) = this.navigate(direction) {
        // Pop up to the start destination of the graph to
        // avoid building up a large stack of destinations
        // on the back stack as users select items
        popUpTo(route) {
            saveState = true
        }

        // Avoid multiple copies of the same destination when
        // re-selecting the same item
        launchSingleTop = true
        // Restore state when re-selecting a previously selected item
        restoreState = true
    }