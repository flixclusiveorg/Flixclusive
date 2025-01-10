package com.flixclusive.feature.mobile.settings.screen.root

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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.flixclusive.core.ui.common.CommonTopBar
import com.flixclusive.core.ui.common.CommonTopBarDefaults.getAdaptiveTopBarHeight
import com.flixclusive.core.ui.common.adaptive.AdaptiveIcon
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveUiUtil.getAdaptiveDp
import com.flixclusive.core.ui.common.util.clearFocusOnSoftKeyboardHide
import com.flixclusive.core.ui.common.util.createTextFieldValue
import com.flixclusive.core.ui.common.util.ifElse
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.ui.common.util.showSoftKeyboard
import com.flixclusive.feature.mobile.settings.util.LocalSettingsSearchQuery
import com.flixclusive.core.locale.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR

@Composable
internal fun DetailsScaffold(
    navigateBack: () -> Unit,
    isListAndDetailVisible: Boolean,
    isDetailsVisible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = MaterialTheme.shapes.medium
    val surface = MaterialTheme.colorScheme.surface
    val brush =
        Brush.verticalGradient(
            0.6F to surface,
            1F to MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
        )

    val searchQuery = remember { mutableStateOf("") }
    val isSearching = remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            if (isDetailsVisible) {
                CommonTopBar(
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (isSearching.value) {
                                    isSearching.value = false
                                } else {
                                    navigateBack()
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
                    body = {},
                    actions = {
                        Box(
                            modifier = Modifier.weight(1F),
                            contentAlignment = Alignment.CenterEnd,
                        ) {
                            AnimatedContent(
                                targetState = isSearching.value,
                                label = "TopBarAction",
                                transitionSpec = { getSearchTransition() },
                            ) { state ->
                                val heightModifier =
                                    Modifier
                                        .height(getAdaptiveTopBarHeight())
                                        .padding(getAdaptiveDp(8.dp))

                                if (state) {
                                    TopBarTextField(
                                        searchQuery = searchQuery.value,
                                        onQueryChange = { searchQuery.value = it },
                                        modifier = heightModifier.fillMaxWidth(),
                                    )
                                } else {
                                    IconButton(
                                        onClick = { isSearching.value = true },
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
        },
    ) { padding ->
        CompositionLocalProvider(
            LocalSettingsSearchQuery provides searchQuery,
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(top = padding.calculateTopPadding())
                        .ifElse(
                            condition = isListAndDetailVisible,
                            ifTrueModifier =
                                Modifier
                                    .padding(UserScreenHorizontalPadding)
                                    .background(brush = brush, shape = shape),
                        ),
                content = content,
            )
        }
    }
}

@Composable
private fun TopBarTextField(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val textFieldValue = remember { mutableStateOf(searchQuery.createTextFieldValue()) }

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
                        textFieldValue.value = "".createTextFieldValue()
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
                    // Expand horizontally first.
                    IntSize(targetSize.width, initialSize.height) at 150
                    durationMillis = 600
                }
            } else {
                keyframes {
                    // Shrink vertically first.
                    IntSize(targetSize.width, initialSize.height) at 150
                    durationMillis = 800
                }
            }
        }
}

@Preview
@Composable
private fun DetailsScaffoldBasePreview() {
    FlixclusiveTheme {
        Surface {
            DetailsScaffold(
                navigateBack = {},
                isListAndDetailVisible = false,
                isDetailsVisible = true,
                content = {},
            )
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun DetailsScaffoldCompactLandscapePreview() {
    DetailsScaffoldBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun DetailsScaffoldMediumPortraitPreview() {
    DetailsScaffoldBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun DetailsScaffoldMediumLandscapePreview() {
    DetailsScaffoldBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun DetailsScaffoldExtendedPortraitPreview() {
    DetailsScaffoldBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun DetailsScaffoldExtendedLandscapePreview() {
    DetailsScaffoldBasePreview()
}
