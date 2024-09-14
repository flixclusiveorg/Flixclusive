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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.platform.LocalContext
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
import com.flixclusive.core.ui.common.util.DummyDataForPreview.getDummyProviderData
import com.flixclusive.core.ui.common.util.createTextFieldValue
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.provider.filter.FilterList
import com.flixclusive.data.tmdb.TmdbFilters.Companion.getDefaultTmdbFilters
import com.flixclusive.feature.mobile.searchExpanded.SearchItemViewType
import com.flixclusive.feature.mobile.searchExpanded.component.filter.ProviderFilterButton
import com.flixclusive.feature.mobile.searchExpanded.util.FilterHelper
import com.flixclusive.feature.mobile.searchExpanded.util.FilterHelper.getFormattedName
import com.flixclusive.feature.mobile.searchExpanded.util.FilterHelper.isBeingUsed
import com.flixclusive.model.provider.ProviderData
import com.flixclusive.core.ui.common.R as UiCommonR
import com.flixclusive.core.locale.R as LocaleR

@Composable
internal fun SearchBarInput(
    currentViewType: MutableState<SearchItemViewType>,
    providerData: ProviderData,
    searchQuery: String,
    lastQuerySearched: String,
    filters: FilterList,
    onSearch: () -> Unit,
    onNavigationIconClick: () -> Unit,
    onToggleFilterSheet: (Int) -> Unit,
    onQueryChange: (String) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current

    var isError by remember { mutableStateOf(false) }
    var textFieldValue by remember(searchQuery) {
        mutableStateOf(searchQuery.createTextFieldValue())
    }
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
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)
    ) {
        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 10.dp)
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
                        contentDescription = stringResource(LocaleR.string.navigate_up)
                    )
                }
            },
            placeholder = {
                Text(
                    text = stringResource(LocaleR.string.search_suggestion),
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
                        text = stringResource(LocaleR.string.empty_query_error_msg),
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
                            contentDescription = stringResource(LocaleR.string.clear_text_button)
                        )
                    }
                }
            },
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                ProviderFilterButton(
                    currentViewType = currentViewType,
                    providerData = providerData
                )
            }

            if (filters.isEmpty()) {
                item {
                    OutlinedButton(
                        onClick = {},
                        contentPadding = PaddingValues(horizontal = 0.dp),
                        shape = MaterialTheme.shapes.small,
                        enabled = false,
                        modifier = Modifier
                            .height(32.dp)
                            .width(40.dp)
                    ) {
                        Icon(
                            painter = painterResource(UiCommonR.drawable.filter_list_off),
                            contentDescription = stringResource(LocaleR.string.filter_button)
                        )
                    }
                }
            }

            itemsIndexed(filters) { i, filterGroup ->
                val isBeingUsed = remember(filterGroup) { filterGroup.isBeingUsed() }

                OutlinedButton(
                    onClick = { onToggleFilterSheet(i) },
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    shape = MaterialTheme.shapes.small,
                    colors = FilterHelper.getButtonColors(isBeingUsed = isBeingUsed),
                    border = FilterHelper.getButtonBorders(isBeingUsed = isBeingUsed),
                    modifier = Modifier
                        .height(32.dp)
                        .widthIn(min = 80.dp)
                ) {
                    AnimatedContent(
                        targetState = filterGroup.getFormattedName(context = context),
                        label = ""
                    ) {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            ),
                        )
                    }
                }
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
                onToggleFilterSheet = {},
                filters = getDefaultTmdbFilters(),
                providerData = getDummyProviderData(),
                currentViewType = remember { mutableStateOf(SearchItemViewType.SearchHistory) }
            )
        }
    }
}