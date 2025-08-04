package com.flixclusive.core.ui.mobile.component.topbar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.adaptive.AdaptiveIcon
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveUiUtil.getAdaptiveDp
import com.flixclusive.core.ui.common.util.clearFocusOnSoftKeyboardHide
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.ui.common.util.showSoftKeyboard
import com.flixclusive.core.ui.common.util.toTextFieldValue
import com.flixclusive.core.ui.mobile.component.PlainTooltipBox
import com.flixclusive.core.ui.mobile.component.textfield.CustomOutlinedTextField
import com.flixclusive.core.ui.mobile.component.topbar.CommonTopBarDefaults.getAdaptiveTopBarHeight
import com.flixclusive.core.ui.mobile.component.topbar.CommonTopBarDefaults.getTopBarHeadlinerTextStyle
import com.flixclusive.core.locale.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR

@Composable
fun CommonTopBarWithSearch(
    isSearching: Boolean,
    searchQuery: () -> String,
    onNavigate: () -> Unit,
    onToggleSearchBar: (Boolean) -> Unit,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    navigationIconColor: Color = LocalContentColor.current,
    titleColor: Color = LocalContentColor.current,
    actionsColor: Color = LocalContentColor.current,
    hideSearchButton: Boolean = false,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    title: String? = null,
    navigationIcon: @Composable () -> Unit = {
        DefaultNavigationIcon(
            onClick = {
                if (isSearching) {
                    onToggleSearchBar(false)
                } else {
                    onNavigate()
                }
            }
        )
    },
    extraActions: @Composable RowScope.() -> Unit = {},
) {
    CommonTopBarWithSearch(
        modifier = modifier,
        hideSearchButton = hideSearchButton,
        scrollBehavior = scrollBehavior,
        navigationIconColor = navigationIconColor,
        titleColor = titleColor,
        actionsColor = actionsColor,
        isSearching = isSearching,
        searchQuery = searchQuery,
        onNavigate = onNavigate,
        onToggleSearchBar = onToggleSearchBar,
        onQueryChange = onQueryChange,
        extraActions = extraActions,
        navigationIcon = navigationIcon,
        titleContent =
            if (title != null) {
                {
                    Text(
                        text = title,
                        style = getTopBarHeadlinerTextStyle(),
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                    )
                }
            } else {
                null
            },
    )
}

@Composable
fun CommonTopBarWithSearch(
    isSearching: Boolean,
    searchQuery: () -> String,
    onNavigate: () -> Unit,
    onToggleSearchBar: (Boolean) -> Unit,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    navigationIconColor: Color = LocalContentColor.current,
    titleColor: Color = LocalContentColor.current,
    actionsColor: Color = LocalContentColor.current,
    hideSearchButton: Boolean = false,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    navigationIcon: @Composable () -> Unit = {
        DefaultNavigationIcon(
            onClick = {
                if (isSearching) {
                    onToggleSearchBar(false)
                } else {
                    onNavigate()
                }
            }
        )
    },
    titleContent: (@Composable () -> Unit)? = null,
    extraActions: @Composable RowScope.() -> Unit = {},
) {
    CommonTopBar(
        modifier = modifier,
        scrollBehavior = scrollBehavior,
        navigationIconColor = navigationIconColor,
        titleColor = titleColor,
        actionsColor = actionsColor,
        navigationIcon = navigationIcon,
        title = {
            titleContent?.let {
                AnimatedVisibility(
                    visible = !isSearching,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    it.invoke()
                }
            }
        },
        actions = {
            SearchTextFieldAction(
                isSearching = isSearching,
                hideSearchButton = hideSearchButton,
                searchQuery = searchQuery,
                onQueryChange = onQueryChange,
                onToggleSearchBar = onToggleSearchBar,
                extraActions = extraActions,
            )
        },
    )
}

@Composable
private fun RowScope.SearchTextFieldAction(
    isSearching: Boolean,
    hideSearchButton: Boolean,
    searchQuery: () -> String,
    onQueryChange: (String) -> Unit,
    onToggleSearchBar: (Boolean) -> Unit,
    extraActions: @Composable RowScope.() -> Unit = {},
) {
    AnimatedContent(
        targetState = isSearching,
        label = "TopBarAction",
        transitionSpec = { getSearchTransition() },
    ) { state ->
        val heightModifier =
            Modifier
                .height(getAdaptiveTopBarHeight())

        if (state) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Spacer(modifier = Modifier.minimumInteractiveComponentSize())

                TopBarTextField(
                    searchQuery = searchQuery,
                    onQueryChange = onQueryChange,
                    modifier = heightModifier
                        .padding(getAdaptiveDp(8.dp))
                        .weight(1F),
                )
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                extraActions()

                AnimatedVisibility(
                    visible = !hideSearchButton
                ) {
                    PlainTooltipBox(description = stringResource(LocaleR.string.search)) {
                        ActionButton(
                            onClick = { onToggleSearchBar(true) },
                            modifier = heightModifier,
                        ) {
                            AdaptiveIcon(
                                painter = painterResource(UiCommonR.drawable.search_outlined),
                                contentDescription = stringResource(LocaleR.string.search),
                                dp = 18.dp,
                                increaseBy = 3.dp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBarTextField(
    searchQuery: () -> String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val textFieldValue = remember { mutableStateOf(searchQuery().toTextFieldValue()) }

    val focusManager = LocalFocusManager.current
    val keyboardManager = LocalSoftwareKeyboardController.current

    CustomOutlinedTextField(
        value = textFieldValue.value,
        onValueChange = {
            textFieldValue.value = it
            onQueryChange(it.text)
        },
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
            ),
        leadingIcon = {
            AdaptiveIcon(
                painter = painterResource(UiCommonR.drawable.search_outlined),
                contentDescription = stringResource(LocaleR.string.search),
                dp = 18.dp,
                increaseBy = 3.dp,
            )
        },
        trailingIcon = {
            AnimatedVisibility(
                visible = textFieldValue.value.text.isNotEmpty(),
                enter = scaleIn(),
                exit = scaleOut(),
            ) {
                ActionButton(
                    onClick = {
                        textFieldValue.value = "".toTextFieldValue()
                        onQueryChange("")
                    },
                ) {
                    AdaptiveIcon(
                        painter = painterResource(UiCommonR.drawable.round_close_24),
                        contentDescription = stringResource(LocaleR.string.close_label),
                        tint = LocalContentColor.current.onMediumEmphasis(),
                    )
                }
            }
        },
        placeholder = {
            Text(
                text = stringResource(LocaleR.string.search),
                style = MaterialTheme.typography.bodyMedium,
                color = LocalContentColor.current.onMediumEmphasis(),
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        },
        textStyle = MaterialTheme.typography.bodyMedium,
        singleLine = true,
        modifier =
            modifier
                .showSoftKeyboard(true)
                .clearFocusOnSoftKeyboardHide(),
        keyboardOptions =
            KeyboardOptions(
                imeAction = ImeAction.Search,
            ),
        keyboardActions =
            KeyboardActions(
                onSearch = {
                    focusManager.clearFocus()
                    keyboardManager?.hide()
                },
            ),
    )
}

private fun AnimatedContentTransitionScope<Boolean>.getSearchTransition(): ContentTransform {
    return fadeIn(animationSpec = tween(300)) togetherWith
        fadeOut(animationSpec = tween(150)) using
        SizeTransform { initialSize, targetSize ->
            if (targetState) {
                keyframes {
                    IntSize(targetSize.width, initialSize.height) at 150
                    durationMillis = 600
                }
            } else {
                keyframes {
                    IntSize(targetSize.width, initialSize.height) at 150
                    durationMillis = 800
                }
            }
        }
}

@Preview
@Composable
private fun CommonTopBarWithSearchBasePreview() {
    var isSearching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    FlixclusiveTheme {
        Surface {
            Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
                    CommonTopBarWithSearch(
                        title = "Test",
                        isSearching = isSearching,
                        searchQuery = { searchQuery },
                        onNavigate = {},
                        onToggleSearchBar = { isSearching = it },
                        onQueryChange = { searchQuery = it },
                        scrollBehavior = scrollBehavior,
                        extraActions = {
                            ActionButton(onClick = {}) {
                                AdaptiveIcon(
                                    painter = painterResource(UiCommonR.drawable.filter_list),
                                    contentDescription = null,
                                    tint = LocalContentColor.current.onMediumEmphasis(),
                                )
                            }
                        },
                    )
                },
            ) {
                LazyColumn(
                    modifier = Modifier.padding(it),
                ) {
                    items(100) { i ->
                        Text(
                            text = "Item $i",
                            modifier =
                                Modifier
                                    .minimumInteractiveComponentSize()
                                    .fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun CommonTopBarWithSearchCompactLandscapePreview() {
    CommonTopBarWithSearchBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun CommonTopBarWithSearchMediumPortraitPreview() {
    CommonTopBarWithSearchBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun CommonTopBarWithSearchMediumLandscapePreview() {
    CommonTopBarWithSearchBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun CommonTopBarWithSearchExtendedPortraitPreview() {
    CommonTopBarWithSearchBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun CommonTopBarWithSearchExtendedLandscapePreview() {
    CommonTopBarWithSearchBasePreview()
}
