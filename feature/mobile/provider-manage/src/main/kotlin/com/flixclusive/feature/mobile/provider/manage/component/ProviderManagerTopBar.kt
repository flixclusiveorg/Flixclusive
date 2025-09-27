package com.flixclusive.feature.mobile.provider.manage.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.presentation.mobile.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.core.presentation.mobile.components.material3.topbar.CommonTopBarDefaults.getTopBarHeadlinerTextStyle
import com.flixclusive.core.presentation.mobile.components.material3.topbar.CommonTopBarWithSearch
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProviderManagerTopBar(
    isSearching: Boolean,
    searchQuery: () -> String,
    tooltipState: TooltipState,
    onNavigationClick: () -> Unit,
    onToggleSearchBar: (Boolean) -> Unit,
    onQueryChange: (String) -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    onNeedHelp: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    CommonTopBarWithSearch(
        isSearching = isSearching,
        searchQuery = searchQuery,
        onQueryChange = onQueryChange,
        onToggleSearchBar = onToggleSearchBar,
        onNavigate = onNavigationClick,
        scrollBehavior = scrollBehavior,
        titleContent = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 15.dp, start = 5.dp),
            ) {
                Text(
                    text = stringResource(id = LocaleR.string.providers),
                    style = getTopBarHeadlinerTextStyle(),
                )

                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = {
                        RichTooltip(
                            title = {
                                Text(
                                    text = stringResource(id = LocaleR.string.understanding_providers),
                                    style = LocalTextStyle.current.asAdaptiveTextStyle(18.sp),
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
                                    Text(
                                        stringResource(id = LocaleR.string.ok),
                                        style = LocalTextStyle.current.asAdaptiveTextStyle(),
                                    )
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
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun ProviderManagerTopBarPreview() {
    var isSearching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    var isFabExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(scrollBehavior.state.heightOffset) {
        delay(800)
        isFabExpanded = scrollBehavior.state.heightOffset < 0f
    }

    FlixclusiveTheme {
        Surface {
            Scaffold(
                modifier = Modifier
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                contentWindowInsets = WindowInsets(0.dp),
                topBar = {
                    ProviderManagerTopBar(
                        isSearching = isSearching,
                        searchQuery = { "" },
                        tooltipState = rememberTooltipState(),
                        onNavigationClick = {},
                        onToggleSearchBar = { isSearching = it },
                        onQueryChange = { searchQuery = it },
                        scrollBehavior = scrollBehavior,
                        onNeedHelp = { },
                    )
                },
                floatingActionButton = {
                    ExtendedFloatingActionButton(
                        onClick = { },
                        expanded = isFabExpanded,
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
                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(it),
                ) {
                    items(50) { i ->
                        Text(
                            text = "Item $i",
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}
