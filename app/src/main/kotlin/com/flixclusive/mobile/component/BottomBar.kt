package com.flixclusive.mobile.component

import android.content.res.Configuration
import androidx.compose.animation.Crossfade
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
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.flixclusive.AppNavigationItem
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.mobile.MobileNavGraphs
import com.ramcosta.composedestinations.spec.NavGraphSpec
import com.flixclusive.core.locale.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR

@Composable
internal fun BottomBar(
    currentSelectedScreen: NavGraphSpec,
    onNavigate: (NavGraphSpec) -> Unit
) {
    val insets = when(LocalConfiguration.current.orientation) {
        Configuration.ORIENTATION_LANDSCAPE ->  WindowInsets(0.dp)
        else -> WindowInsets.navigationBars
    }

    Box(
        modifier = Modifier
            .windowInsetsPadding(insets)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
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
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            mobileNavigationItems.forEach {
                CustomNavItem(
                    item = it,
                    isSelected = currentSelectedScreen == it.screen,
                    onClick = {
                        onNavigate(it.screen)
                    }
                )
            }
        }
    }
}

@Composable
private fun CustomNavItem(
    item: AppNavigationItem,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }

    val unselectedContentColor = LocalContentColor.current.onMediumEmphasis()
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
                indication = null,
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
                        indication = ripple()
                    )
            )

            Crossfade(
                targetState = isSelected,
                label = ""
            ) {
                Icon(
                    painter = if(it) painterResource(item.iconSelected) else painterResource(item.iconUnselected),
                    contentDescription = stringResource(id = item.label)
                )
            }
        }

        Text(
            text = stringResource(id = item.label),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelSmall.copy(
                color = contentColor,
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier
                .padding(bottom = 1.5.dp)
        )
    }
}


private val mobileNavigationItems = listOf(
    AppNavigationItem(
        screen = MobileNavGraphs.home,
        iconSelected = UiCommonR.drawable.home,
        iconUnselected = UiCommonR.drawable.home_outlined,
        label = LocaleR.string.home
    ),
    AppNavigationItem(
        screen = MobileNavGraphs.search,
        iconSelected = UiCommonR.drawable.search,
        iconUnselected = UiCommonR.drawable.search_outlined,
        label = LocaleR.string.search
    ),
    AppNavigationItem(
        screen = MobileNavGraphs.providers,
        iconSelected = UiCommonR.drawable.provider_logo_fill,
        iconUnselected = UiCommonR.drawable.provider_logo,
        label = LocaleR.string.providers
    ),
    AppNavigationItem(
        screen = MobileNavGraphs.preferences,
        iconSelected = UiCommonR.drawable.settings_filled,
        iconUnselected = UiCommonR.drawable.settings,
        label = LocaleR.string.preferences
    ),
)
