package com.flixclusive.feature.mobile.provider.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.util.createTextFieldValue
import com.flixclusive.core.ui.common.util.noIndicationClickable
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.ui.common.R as UiCommonR
import com.flixclusive.core.util.R as UtilR


private val SearchIconSize = 18.dp

@Composable
internal fun ProvidersTopBar(
    isVisible: Boolean,
    searchExpanded: MutableState<Boolean>,
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onNeedHelp: () -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
    ) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .noIndicationClickable {},
            contentAlignment = Alignment.TopCenter
        ) {
            Crossfade(
                targetState = searchExpanded.value,
                label = ""
            ) {
                when(it) {
                    true -> {
                        Box(
                            modifier = Modifier
                                .statusBarsPadding()
                        ) {
                            ExpandedTopBar(
                                searchQuery = searchQuery,
                                onQueryChange = onQueryChange,
                                onCollapseTopBar = { searchExpanded.value = false }
                            )
                        }
                    }
                    false -> {
                        CollapsedTopBar(
                            onExpandTopBar = { searchExpanded.value = true },
                            onNeedHelp = onNeedHelp,
                            modifier = Modifier
                                .statusBarsPadding()
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CollapsedTopBar(
    modifier: Modifier = Modifier,
    onExpandTopBar: () -> Unit,
    onNeedHelp: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1F)
                .padding(end = 15.dp, start = 5.dp)
        ) {
            Text(
                text = stringResource(id = UtilR.string.providers),
                style = MaterialTheme.typography.headlineMedium,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )

            TooltipBox(
                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                tooltip = {
                    PlainTooltip {
                        Text(stringResource(id = UtilR.string.help))
                    }
                },
                state = rememberTooltipState(isPersistent = true)
            ) {
                Box(
                    modifier = Modifier.clickable {
                        onNeedHelp()
                    },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = UiCommonR.drawable.help),
                        contentDescription = stringResource(id = UtilR.string.help)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .size(SearchIconSize.times(2))
                .clickable(
                    onClick = onExpandTopBar,
                    role = Role.Button,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(
                        bounded = false,
                        radius = SearchIconSize
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = UiCommonR.drawable.search_outlined),
                contentDescription = stringResource(id = UtilR.string.search_for_providers),
                modifier = Modifier
                    .size(SearchIconSize)
            )
        }
    }
}

@Composable
private fun ExpandedTopBar(
    modifier: Modifier = Modifier,
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onCollapseTopBar: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val keyboardManager = LocalSoftwareKeyboardController.current
    val textFieldFocusRequester = remember { FocusRequester() }

    SideEffect {
        textFieldFocusRequester.requestFocus()
    }

    var textFieldValue by remember {
        mutableStateOf(searchQuery.createTextFieldValue())
    }

    OutlinedTextField(
        value = textFieldValue,
        onValueChange = {
            textFieldValue = it
            onQueryChange(it.text)
        },
        colors = OutlinedTextFieldDefaults.colors(),
        leadingIcon = {
            Icon(
                painter = painterResource(UiCommonR.drawable.search_outlined),
                contentDescription = stringResource(UtilR.string.search),
            )
        },
        trailingIcon = {
            IconButton(onClick = onCollapseTopBar) {
                Icon(
                    painter = painterResource(UiCommonR.drawable.round_close_24),
                    contentDescription = stringResource(UtilR.string.close_label)
                )
            }
        },
        placeholder = {
            Text(
                text = stringResource(UtilR.string.search_for_providers),
                style = MaterialTheme.typography.bodyMedium,
                color = LocalContentColor.current.onMediumEmphasis(),
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )
        },
        textStyle = MaterialTheme.typography.bodyMedium,
        singleLine = true,
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(textFieldFocusRequester),
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Search
        ),
        keyboardActions = KeyboardActions(
            onSearch = {
                focusManager.clearFocus()
                keyboardManager?.hide()
            }
        )
    )
}


@Preview
@Composable
private fun TopBarPreview() {
    FlixclusiveTheme {
        Surface {
            Scaffold(
                contentWindowInsets = WindowInsets(0.dp),
                topBar = {
                    ProvidersTopBar(
                        isVisible = true,
                        searchExpanded = remember { mutableStateOf(false) },
                        searchQuery = "",
                        onNeedHelp = {},
                        onQueryChange = {}
                    )
                },
                floatingActionButton = {
                    ExtendedFloatingActionButton(
                        onClick = { /*TODO*/ },
                        containerColor = MaterialTheme.colorScheme.surface,
                        text = {
                            Text(text = stringResource(UtilR.string.add_provider))
                        },
                        icon = {
                            Icon(
                                painter = painterResource(id = UiCommonR.drawable.round_add_24),
                                contentDescription = stringResource(UtilR.string.add_provider)
                            )
                        }
                    )
                }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(it)
                ) {

                }
            }
        }
    }
}