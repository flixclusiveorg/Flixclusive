package com.flixclusive.feature.mobile.searchExpanded.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.util.createTextFieldValue
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.feature.mobile.searchExpanded.SearchItemViewType
import com.flixclusive.model.tmdb.DEFAULT_FILM_SOURCE_NAME
import com.flixclusive.core.ui.common.R as UiCommonR
import com.flixclusive.core.util.R as UtilR

@Composable
internal fun SearchBarInput(
    currentViewType: MutableState<SearchItemViewType>,
    selectedProvider: String,
    searchQuery: String,
    lastQuerySearched: String,
    onSearch: () -> Unit,
    onNavigationIconClick: () -> Unit,
    onChangeProvider: (Int) -> Unit,
    onQueryChange: (String) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    var isError by remember { mutableStateOf(false) }
    var textFieldValue by remember(searchQuery) {
        mutableStateOf(searchQuery.createTextFieldValue())
    }

    var lastViewTypeSelected by remember { mutableStateOf(currentViewType.value) }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(searchQuery, lastQuerySearched) {
        val isTypingNewQuery = searchQuery != lastQuerySearched

        if (isTypingNewQuery) {
            currentViewType.value = SearchItemViewType.SearchHistory
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 10.dp)
                .padding(horizontal = 10.dp)
                .focusRequester(focusRequester),
            value = textFieldValue,
            onValueChange = {
                textFieldValue = it

                isError = false
                onQueryChange(it.text)
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium,
            keyboardActions = KeyboardActions(
                onSearch = {
                    keyboardController?.hide()

                    if(textFieldValue.text.isEmpty()) {
                        isError = true
                    } else {
                        currentViewType.value = SearchItemViewType.Films
                    }

                    onSearch()
                }
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            shape = MaterialTheme.shapes.small,
            colors = TextFieldDefaults.colors(
                disabledTextColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
            ),
            leadingIcon = {
                IconButton(onClick = onNavigationIconClick) {
                    Icon(
                        painter = painterResource(UiCommonR.drawable.left_arrow),
                        contentDescription = stringResource(UtilR.string.navigate_up)
                    )
                }
            },
            placeholder = {
                Text(
                    text = stringResource(UtilR.string.search_suggestion),
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalContentColor.current.onMediumEmphasis(),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
                )
            },
            supportingText = {
                if (isError) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(UtilR.string.empty_query_error_msg),
                        color = MaterialTheme.colorScheme.error,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            },
            trailingIcon = {
                this@Column.AnimatedVisibility(
                    visible = textFieldValue.text.isNotEmpty(),
                    enter = scaleIn(),
                    exit = scaleOut(),
                ) {
                    IconButton(
                        onClick = { onQueryChange("") }
                    ) {
                        Icon(
                            painter = painterResource(UiCommonR.drawable.outline_close_square),
                            contentDescription = stringResource(UtilR.string.clear_text_button)
                        )
                    }
                }
            },
        )

        OutlinedButton(
            onClick = {
                currentViewType.value = when (currentViewType.value) {
                    SearchItemViewType.Providers -> lastViewTypeSelected
                    else -> {
                        lastViewTypeSelected = currentViewType.value
                        SearchItemViewType.Providers
                    }
                }
            },
            contentPadding = PaddingValues(horizontal = 12.dp),
            shape = MaterialTheme.shapes.small,
            modifier = Modifier
                .height(32.dp)
                .widthIn(min = 200.dp)
        ) {
            AnimatedContent(
                targetState = selectedProvider,
                label = "",
            ) {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                )
            }
        }

        Spacer(modifier = Modifier.height(15.dp))
    }
}

@Preview
@Composable
private fun SearchBarExpandedPreview() {
    FlixclusiveTheme {
        Surface {
            SearchBarInput(
                searchQuery = "Star Wars",
                lastQuerySearched = "Iron Man",
                onSearch = {},
                onNavigationIconClick = {},
                onQueryChange = {},
                onChangeProvider = {},
                selectedProvider = DEFAULT_FILM_SOURCE_NAME,
                currentViewType = remember { mutableStateOf(SearchItemViewType.SearchHistory) }
            )
        }
    }
}