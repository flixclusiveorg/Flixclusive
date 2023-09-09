package com.flixclusive.presentation.mobile.main

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.flixclusive.presentation.destinations.Destination
import com.flixclusive.presentation.mobile.utils.ComposeMobileUtils.colorOnMediumEmphasisMobile
import com.ramcosta.composedestinations.spec.Direction
import com.ramcosta.composedestinations.spec.NavGraphSpec

val NAVIGATION_BAR_HEIGHT = 60.dp

@Composable
fun MainNavigationBar(
    currentScreen: Destination,
    onNavigate: (Direction) -> Unit,
    onButtonClickTwice: (NavGraphSpec) -> Unit,
) {
    Box(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(NAVIGATION_BAR_HEIGHT)
                .background(MaterialTheme.colorScheme.surface)
                .drawBehind {
                    val strokeWidth = 2F
                    val x = size.width - strokeWidth

                    drawLine(
                        color = Color.LightGray,
                        start = Offset(0F, 0F),
                        end = Offset(x, 0F),
                        strokeWidth = strokeWidth
                    )
                },
            horizontalArrangement = Arrangement.spacedBy(55.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MobileAppDestination.values().forEach {
                val icon = remember(currentScreen) {
                    if (currentScreen == it.direction) {
                        it.iconSelected
                    } else it.iconUnselected
                }

                CustomNavItem(
                    iconId = icon,
                    label = stringResource(id = it.label),
                    isSelected = currentScreen == it.direction,
                    onClick = {
                        if (it.direction == currentScreen) {
                            onButtonClickTwice(it.navGraph)
                            return@CustomNavItem
                        }

                        onNavigate(it.direction)
                    }
                )
            }
        }
    }
}

@Composable
private fun CustomNavItem(
    @DrawableRes iconId: Int,
    isSelected: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }

    val unselectedContentColor = colorOnMediumEmphasisMobile()
    val contentColor = remember(isSelected) {
        if(isSelected) Color.White
        else unselectedContentColor
    }

    Column(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .size(64.dp)
            .background(color = Color.Transparent)
            .clickable(
                onClick = onClick,
                role = Role.Button,
                interactionSource = interactionSource,
                indication = null
            ),
        verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .clip(CircleShape)
                    .width(50.dp)
                    .height(28.dp)
                    .indication(
                        interactionSource = interactionSource,
                        indication = rememberRipple()
                    )
            )

            Icon(
                painter = painterResource(iconId),
                contentDescription = label
            )
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = contentColor,
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier
                .padding(bottom = 1.5.dp)
        )
    }
}