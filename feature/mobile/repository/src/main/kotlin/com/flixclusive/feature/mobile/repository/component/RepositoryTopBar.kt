package com.flixclusive.feature.mobile.repository.component

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.util.createTextFieldValue
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.ui.common.R as UiCommonR
import com.flixclusive.core.locale.R as LocaleR

@SuppressLint("UnusedCrossfadeTargetStateParameter")
@Composable
internal fun RepositoryTopBar(
    isVisible: Boolean,
    searchExpanded: MutableState<Boolean>,
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onNavigationIconClick: () -> Unit,
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
    ) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .statusBarsPadding()
                .height(65.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Crossfade(
                targetState = searchExpanded.value,
                label = ""
            ) {
                when(it) {
                    true -> {
                        ExpandedTopBar(
                            searchQuery = searchQuery,
                            onQueryChange = onQueryChange,
                            onCollapseTopBar = { searchExpanded.value = false }
                        )
                    }
                    false -> {
                        CollapsedTopBar(
                            onNavigationIconClick = onNavigationIconClick,
                            onExpandTopBar = { searchExpanded.value = true }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CollapsedTopBar(
    modifier: Modifier = Modifier,
    onNavigationIconClick: () -> Unit,
    onExpandTopBar: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onNavigationIconClick) {
            Icon(
                painter = painterResource(UiCommonR.drawable.left_arrow),
                contentDescription = stringResource(LocaleR.string.navigate_up)
            )
        }

        Text(
            text = stringResource(id = LocaleR.string.repository),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.SemiBold
            ),
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            modifier = Modifier
                .weight(1F)
                .padding(horizontal = 15.dp)
        )

        IconButton(onClick = onExpandTopBar) {
            Icon(
                painter = painterResource(UiCommonR.drawable.search_outlined),
                contentDescription = stringResource(LocaleR.string.search_for_providers),
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
                contentDescription = stringResource(LocaleR.string.search),
            )
        },
        trailingIcon = {
            IconButton(onClick = onCollapseTopBar) {
                Icon(
                    painter = painterResource(UiCommonR.drawable.round_close_24),
                    contentDescription = stringResource(LocaleR.string.close_label)
                )
            }
        },
        placeholder = {
            Text(
                text = stringResource(LocaleR.string.search_for_providers),
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
            .fillMaxHeight()
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
private fun RepositoryTopBarPreview() {
    FlixclusiveTheme {
        Surface {
            RepositoryTopBar(
                isVisible = true,
                searchExpanded = remember { mutableStateOf(false) },
                searchQuery = "",
                onQueryChange = {}
            ) {

            }
        }
    }
}