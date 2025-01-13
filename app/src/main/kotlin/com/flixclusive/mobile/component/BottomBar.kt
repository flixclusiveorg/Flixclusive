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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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

internal val MaxBottomBarHeight = 60.dp

@Composable
internal fun BottomBar(
    currentSelectedScreen: NavGraphSpec,
    onNavigate: (NavGraphSpec) -> Unit,
) {
    Box(
        modifier =
            Modifier
                .background(MaterialTheme.colorScheme.surface)
                .windowInsetsPadding(getProperInsets()),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(MaxBottomBarHeight)
                    .background(MaterialTheme.colorScheme.surface)
                    .drawBehind {
                        val strokeWidth = 2F
                        val x = size.width - strokeWidth

                        drawLine(
                            color = Color.LightGray,
                            start = Offset(0F, 0F),
                            end = Offset(x, 0F),
                            strokeWidth = strokeWidth,
                        )
                    },
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            mobileNavigationItems.forEach {
                CustomNavItem(
                    item = it,
                    isSelected = currentSelectedScreen == it.screen,
                    onClick = {
                        onNavigate(it.screen)
                    },
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

    Column(
        modifier =
            Modifier
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
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .clip(CircleShape)
                    .width(50.dp)
                    .height(28.dp)
                    .indication(
                        interactionSource = interactionSource,
                        indication = ripple(),
                    ),
            )

            Crossfade(
                targetState = isSelected,
                label = "",
            ) {
                Icon(
                    painter = if (it) painterResource(item.iconSelected) else painterResource(item.iconUnselected),
                    contentDescription = stringResource(id = item.label),
                )
            }
        }

        Text(
            text = stringResource(id = item.label),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style =
                MaterialTheme.typography.labelSmall.copy(
                    color =
                        if (isSelected) {
                            Color.White
                        } else {
                            unselectedContentColor
                        },
                    fontWeight = FontWeight.Medium,
                ),
            modifier =
                Modifier
                    .padding(bottom = 1.5.dp),
        )
    }
}

@Composable
internal fun getProperInsets(): WindowInsets {
    var orientation by remember { mutableIntStateOf(Configuration.ORIENTATION_PORTRAIT) }
    val configuration = LocalConfiguration.current

    LaunchedEffect(configuration) {
        snapshotFlow { configuration.orientation }
            .collect { orientation = it }
    }

    return when (orientation) {
        Configuration.ORIENTATION_LANDSCAPE -> WindowInsets(0.dp)
        else -> WindowInsets.navigationBars
    }
}

private val mobileNavigationItems =
    listOf(
        AppNavigationItem(
            screen = MobileNavGraphs.home,
            iconSelected = UiCommonR.drawable.home,
            iconUnselected = UiCommonR.drawable.home_outlined,
            label = LocaleR.string.home,
        ),
        AppNavigationItem(
            screen = MobileNavGraphs.search,
            iconSelected = UiCommonR.drawable.search,
            iconUnselected = UiCommonR.drawable.search_outlined,
            label = LocaleR.string.search,
        ),
        AppNavigationItem(
            screen = MobileNavGraphs.library,
            iconSelected = UiCommonR.drawable.library_outline,
            iconUnselected = UiCommonR.drawable.round_library,
            label = LocaleR.string.library,
        ),
        AppNavigationItem(
            screen = MobileNavGraphs.settings,
            iconSelected = UiCommonR.drawable.settings_filled,
            iconUnselected = UiCommonR.drawable.settings,
            label = LocaleR.string.settings,
        ),
    )
