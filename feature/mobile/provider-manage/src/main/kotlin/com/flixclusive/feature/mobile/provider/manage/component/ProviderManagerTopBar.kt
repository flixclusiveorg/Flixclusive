package com.flixclusive.feature.mobile.provider.manage.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TooltipState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.CommonTopBarWithSearch
import com.flixclusive.core.ui.common.TOP_BAR_BODY_FADE_DURATION
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveStylesUtil.getAdaptiveTextStyle
import com.flixclusive.core.ui.common.util.adaptive.TextStyleMode
import com.flixclusive.core.ui.common.util.adaptive.TypographyStyle
import com.flixclusive.core.ui.common.util.ifElse
import kotlinx.coroutines.launch
import com.flixclusive.core.locale.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProviderManagerTopBar(
    isVisible: Boolean,
    isSearching: Boolean,
    searchQuery: String,
    tooltipState: TooltipState,
    onNavigationClick: () -> Unit,
    onToggleSearchBar: (Boolean) -> Unit,
    onQueryChange: (String) -> Unit,
    onNeedHelp: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
    ) {
        CommonTopBarWithSearch(
            isSearching = isSearching,
            searchQuery = searchQuery,
            onQueryChange = onQueryChange,
            onToggleSearchBar = onToggleSearchBar,
            onNavigateBack = onNavigationClick,
            body = {
                AnimatedVisibility(
                    visible = !isSearching,
                    enter = fadeIn(),
                    exit = fadeOut(tween(TOP_BAR_BODY_FADE_DURATION)),
                    modifier =
                        Modifier.ifElse(
                            condition = !isSearching,
                            ifTrueModifier = Modifier.weight(1F),
                        ),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 15.dp, start = 5.dp),
                    ) {
                        Text(
                            text = stringResource(id = LocaleR.string.providers),
                            style =
                                getAdaptiveTextStyle(
                                    mode = TextStyleMode.Normal,
                                    style = TypographyStyle.Body,
                                ).copy(fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                        )

                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                            tooltip = {
                                RichTooltip(
                                    title = {
                                        Text(
                                            text = stringResource(id = LocaleR.string.understanding_providers),
                                            style =
                                                LocalTextStyle.current.copy(
                                                    fontSize = 18.sp,
                                                ),
                                        )
                                    },
                                    action = {
                                        TextButton(
                                            onClick = {
                                                scope.launch {
                                                    tooltipState.dismiss()
                                                }
                                            },
                                        ) {
                                            Text(stringResource(id = LocaleR.string.ok))
                                        }
                                    },
                                ) {
                                    Text(
                                        text = stringResource(id = LocaleR.string.tooltip_providers_screen_help_guide),
                                    )
                                }
                            },
                            state = tooltipState,
                        ) {
                            Box(
                                modifier =
                                    Modifier.clickable {
                                        onNeedHelp()
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    painter = painterResource(id = UiCommonR.drawable.help),
                                    contentDescription = stringResource(id = LocaleR.string.help),
                                )
                            }
                        }
                    }
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun TopBarPreview() {
    var isSearching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    FlixclusiveTheme {
        Surface {
            Scaffold(
                contentWindowInsets = WindowInsets(0.dp),
                topBar = {
                    ProviderManagerTopBar(
                        isVisible = true,
                        isSearching = isSearching,
                        searchQuery = "",
                        tooltipState = rememberTooltipState(),
                        onNavigationClick = {},
                        onToggleSearchBar = { isSearching = it },
                        onQueryChange = { searchQuery = it },
                        onNeedHelp = { },
                    )
                },
                floatingActionButton = {
                    ExtendedFloatingActionButton(
                        onClick = { },
                        containerColor = MaterialTheme.colorScheme.surface,
                        text = {
                            Text(text = stringResource(LocaleR.string.manage_providers))
                        },
                        icon = {
                            Icon(
                                painter = painterResource(id = UiCommonR.drawable.round_add_24),
                                contentDescription = stringResource(LocaleR.string.manage_providers),
                            )
                        },
                    )
                },
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(it),
                )
            }
        }
    }
}
