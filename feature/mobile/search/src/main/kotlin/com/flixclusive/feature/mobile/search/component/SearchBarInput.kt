package com.flixclusive.feature.mobile.search.component

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.presentation.common.extensions.toTextFieldValue
import com.flixclusive.core.presentation.common.util.DummyDataForPreview.getProviderMetadata
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.AdaptiveSizeUtil.getAdaptiveDp
import com.flixclusive.core.presentation.mobile.util.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.feature.mobile.search.SearchItemViewType
import com.flixclusive.feature.mobile.search.SearchProvider
import com.flixclusive.feature.mobile.search.component.filter.ProviderFilterButton
import com.flixclusive.feature.mobile.search.util.FilterHelper
import com.flixclusive.feature.mobile.search.util.FilterHelper.getFormattedName
import com.flixclusive.feature.mobile.search.util.FilterHelper.isBeingUsed
import com.flixclusive.provider.filter.FilterList
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

@Composable
internal fun SearchBarInput(
    currentViewType: SearchItemViewType,
    provider: SearchProvider?,
    searchQuery: () -> String,
    lastQuerySearched: String,
    filters: FilterList,
    onSearch: () -> Unit,
    onChangeView: (SearchItemViewType) -> Unit,
    onNavigationIconClick: () -> Unit,
    onToggleFilterSheet: (Int) -> Unit,
    onQueryChange: (String) -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current

    var isError by remember { mutableStateOf(false) }
    var textFieldValue by remember(searchQuery) {
        mutableStateOf(searchQuery().toTextFieldValue())
    }
    val focusRequester = remember { FocusRequester() }
    val isTypingNewQuery by remember {
        derivedStateOf {
            searchQuery() != lastQuerySearched
        }
    }

    LaunchedEffect(Unit) {
        if (provider != null) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    val updatedOnChangeView by rememberUpdatedState(onChangeView)
    LaunchedEffect(isTypingNewQuery) {
        if (isTypingNewQuery) {
            updatedOnChangeView(SearchItemViewType.History)
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp),
    ) {
        OutlinedTextField(
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
            isError = isError,
            enabled = provider != null,
            textStyle = MaterialTheme.typography.bodyMedium,
            keyboardActions = KeyboardActions(
                onSearch = {
                    keyboardController?.hide()

                    if (textFieldValue.text.isEmpty()) {
                        isError = true
                    } else {
                        onChangeView(SearchItemViewType.Medias)
                    }

                    if (isError) return@KeyboardActions

                    onSearch()
                },
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            shape = MaterialTheme.shapes.small,
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(10.dp),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                disabledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
            ),
            leadingIcon = {
                IconButton(onClick = onNavigationIconClick) {
                    Icon(
                        painter = painterResource(UiCommonR.drawable.left_arrow),
                        contentDescription = stringResource(LocaleR.string.back),
                    )
                }
            },
            placeholder = {
                Text(
                    text = stringResource(LocaleR.string.search_text_field_placeholder),
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalContentColor.current.copy(0.6f),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )
            },
            trailingIcon = {
                this@Column.AnimatedVisibility(
                    visible = textFieldValue.text.isNotEmpty(),
                    enter = scaleIn(),
                    exit = scaleOut(),
                ) {
                    IconButton(
                        onClick = {
                            onQueryChange("")
                            textFieldValue = "".toTextFieldValue()
                        },
                    ) {
                        Icon(
                            painter = painterResource(UiCommonR.drawable.outline_close_square),
                            contentDescription = stringResource(LocaleR.string.clear_text_button),
                        )
                    }
                }
            },
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth(),
        ) {
            item {
                ProviderFilterButton(
                    currentViewType = currentViewType,
                    provider = provider?.metadata,
                    onChangeView = onChangeView,
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
                            .height(getAdaptiveDp(32.dp))
                            .widthIn(min = getAdaptiveDp(40.dp)),
                    ) {
                        AdaptiveIcon(
                            painter = painterResource(UiCommonR.drawable.filter_list_off),
                            contentDescription = stringResource(LocaleR.string.filter_button),
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
                    border = ButtonDefaults.outlinedButtonBorder(isBeingUsed),
                    modifier = Modifier
                        .height(getAdaptiveDp(32.dp))
                        .widthIn(min = getAdaptiveDp(80.dp)),
                ) {
                    AnimatedContent(
                        targetState = filterGroup.getFormattedName(context = context),
                        label = "",
                    ) {
                        Text(
                            text = it,
                            fontWeight = FontWeight.Medium,
                            style = MaterialTheme.typography.labelMedium.asAdaptiveTextStyle(14.sp),
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
                searchQuery = { "Star Wars" },
                lastQuerySearched = "Iron Man",
                onSearch = {},
                onNavigationIconClick = {},
                onQueryChange = {},
                onToggleFilterSheet = {},
                filters = FilterList(),
                provider = SearchProvider(getProviderMetadata(), true),
                currentViewType = SearchItemViewType.History,
                onChangeView = {},
            )
        }
    }
}
