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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.adaptive.AdaptiveIcon
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveStylesUtil.getAdaptiveTextStyle
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveUiUtil.getAdaptiveDp
import com.flixclusive.core.ui.common.util.adaptive.TextStyleMode
import com.flixclusive.core.ui.common.util.adaptive.TypographyStyle
import com.flixclusive.core.ui.common.util.clearFocusOnSoftKeyboardHide
import com.flixclusive.core.ui.common.util.ifElse
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.ui.common.util.showSoftKeyboard
import com.flixclusive.core.ui.common.util.toTextFieldValue
import com.flixclusive.core.ui.mobile.component.topbar.CommonTopBarDefaults.getAdaptiveTopBarHeight
import com.flixclusive.core.locale.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR

const val TOP_BAR_BODY_FADE_DURATION = 200

@Composable
fun CommonTopBarWithSearch(
    isSearching: Boolean,
    searchQuery: String,
    onNavigateBack: () -> Unit,
    onToggleSearchBar: (Boolean) -> Unit,
    onQueryChange: (String) -> Unit,
    boxModifier: Modifier = Modifier,
    rowModifier: Modifier = Modifier,
    title: String? = null,
) {
    CommonTopBar(
        boxModifier = boxModifier,
        rowModifier = rowModifier,
        navigationIcon = {
            IconButton(
                onClick = {
                    if (isSearching) {
                        onToggleSearchBar(false)
                    } else {
                        onNavigateBack()
                    }
                },
            ) {
                AdaptiveIcon(
                    painter = painterResource(UiCommonR.drawable.left_arrow),
                    contentDescription = stringResource(LocaleR.string.navigate_up),
                    dp = 16.dp,
                    increaseBy = 3.dp,
                )
            }
        },
        body = {
            AnimatedVisibility(
                visible = title != null && !isSearching,
                enter = fadeIn(),
                exit = fadeOut(tween(TOP_BAR_BODY_FADE_DURATION)),
                modifier =
                    Modifier.ifElse(
                        condition = title != null && !isSearching,
                        ifTrueModifier = Modifier.weight(1F),
                    ),
            ) {
                Text(
                    text = title!!,
                    style =
                        getAdaptiveTextStyle(
                            style = TypographyStyle.Body,
                            mode = TextStyleMode.Normal,
                            size = 20.sp,
                            increaseBy = 5.sp,
                        ).copy(fontWeight = FontWeight.SemiBold),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    modifier =
                        Modifier
                            .padding(start = 15.dp),
                )
            }
        },
        actions = {
            Box(
                modifier =
                    Modifier.ifElse(
                        condition = title == null || isSearching,
                        ifTrueModifier = Modifier.weight(1F),
                    ),
                contentAlignment = Alignment.CenterEnd,
            ) {
                AnimatedContent(
                    targetState = isSearching,
                    label = "TopBarAction",
                    transitionSpec = { getSearchTransition() },
                ) { state ->
                    val heightModifier =
                        Modifier
                            .height(getAdaptiveTopBarHeight())
                            .padding(getAdaptiveDp(8.dp))

                    if (state) {
                        TopBarTextField(
                            searchQuery = searchQuery,
                            onQueryChange = onQueryChange,
                            modifier = heightModifier.fillMaxWidth(),
                        )
                    } else {
                        IconButton(
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
        },
    )
}

@Composable
fun CommonTopBarWithSearch(
    isSearching: Boolean,
    searchQuery: String,
    onNavigateBack: () -> Unit,
    onToggleSearchBar: (Boolean) -> Unit,
    onQueryChange: (String) -> Unit,
    boxModifier: Modifier = Modifier,
    rowModifier: Modifier = Modifier,
    body: (@Composable RowScope.() -> Unit)? = null,
) {
    CommonTopBar(
        boxModifier = boxModifier,
        rowModifier = rowModifier,
        navigationIcon = {
            IconButton(
                onClick = {
                    if (isSearching) {
                        onToggleSearchBar(false)
                    } else {
                        onNavigateBack()
                    }
                },
            ) {
                AdaptiveIcon(
                    painter = painterResource(UiCommonR.drawable.left_arrow),
                    contentDescription = stringResource(LocaleR.string.navigate_up),
                    dp = 16.dp,
                    increaseBy = 3.dp,
                )
            }
        },
        body = { body?.invoke(this) },
        actions = {
            Box(
                modifier =
                    Modifier.ifElse(
                        condition = body == null || isSearching,
                        ifTrueModifier = Modifier.weight(1F),
                    ),
                contentAlignment = Alignment.CenterEnd,
            ) {
                AnimatedContent(
                    targetState = isSearching,
                    label = "TopBarAction",
                    transitionSpec = { getSearchTransition() },
                ) { state ->
                    val heightModifier =
                        Modifier
                            .height(getAdaptiveTopBarHeight())
                            .padding(getAdaptiveDp(8.dp))

                    if (state) {
                        TopBarTextField(
                            searchQuery = searchQuery,
                            onQueryChange = onQueryChange,
                            modifier = heightModifier.fillMaxWidth(),
                        )
                    } else {
                        IconButton(
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
        },
    )
}

@Composable
private fun TopBarTextField(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val textFieldValue = remember { mutableStateOf(searchQuery.toTextFieldValue()) }

    val focusManager = LocalFocusManager.current
    val keyboardManager = LocalSoftwareKeyboardController.current

    OutlinedTextField(
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
                IconButton(
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
    return fadeIn(animationSpec = tween(300, delayMillis = TOP_BAR_BODY_FADE_DURATION)) togetherWith
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

    FlixclusiveTheme {
        Surface {
            CommonTopBarWithSearch(
                title = "Test",
                isSearching = isSearching,
                searchQuery = searchQuery,
                onNavigateBack = {},
                onToggleSearchBar = { isSearching = it },
                onQueryChange = { searchQuery = it },
            )
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
