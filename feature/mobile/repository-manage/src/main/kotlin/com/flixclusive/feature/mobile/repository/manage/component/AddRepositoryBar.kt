@file:Suppress("ktlint:compose:lambda-param-in-effect")

package com.flixclusive.feature.mobile.repository.manage.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.presentation.common.extensions.toTextFieldValue
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.core.presentation.mobile.components.material3.CustomOutlinedTextField
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.AdaptiveSizeUtil.getAdaptiveDp
import com.flixclusive.domain.provider.util.toGithubUrl
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

private val DefaultTextFieldHeight = 50.dp

@Composable
internal fun AddRepositoryBar(
    urlQuery: () -> String,
    isParseError: Boolean,
    focusRequester: FocusRequester,
    onUrlQueryChange: (String) -> Unit,
    onAdd: () -> Unit,
    onConsumeError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val clipboardManager = LocalClipboardManager.current

    var textFieldValue by remember { mutableStateOf(urlQuery().toTextFieldValue()) }

    val containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
    val focusedContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(5.dp)

    var isClipboardParsed by remember { mutableStateOf(false) }
    var textFieldError by remember { mutableStateOf(false) }
    LaunchedEffect(true) {
        if (!isClipboardParsed) {
            val parsedClipboard = clipboardManager
                .getText()
                ?.text
                ?.toGithubUrl()
                ?.toTextFieldValue()

            if (parsedClipboard != null) {
                onUrlQueryChange(parsedClipboard.text)
            }
        }

        isClipboardParsed = true
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        CustomOutlinedTextField(
            value = textFieldValue,
            onValueChange = {
                textFieldError = false
                onConsumeError()

                textFieldValue = it
                onUrlQueryChange(it.text)
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall,
            keyboardActions =
                KeyboardActions(
                    onGo = {
                        focusManager.clearFocus()
                        keyboardController?.hide()

                        if (textFieldValue.text.isEmpty()) {
                            textFieldError = true
                            return@KeyboardActions
                        }

                        onAdd()
                    },
                ),
            isError = isParseError || textFieldError,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            shape = MaterialTheme.shapes.extraSmall,
            colors =
                TextFieldDefaults.colors(
                    focusedContainerColor = focusedContainerColor,
                    unfocusedContainerColor = containerColor,
                    errorContainerColor = containerColor,
                    disabledTextColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                ),
            placeholder = {
                Text(
                    text = stringResource(LocaleR.string.search_repository_url_suggestion),
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalContentColor.current.copy(0.6f),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )
            },
            trailingIcon = {
                this@Row.AnimatedVisibility(
                    visible = textFieldValue.text.isNotEmpty(),
                    enter = scaleIn(),
                    exit = scaleOut(),
                ) {
                    IconButton(onClick = { onUrlQueryChange("") }) {
                        Icon(
                            painter = painterResource(UiCommonR.drawable.round_close_24),
                            contentDescription = stringResource(LocaleR.string.clear_text_button),
                        )
                    }
                }
            },
            modifier = Modifier
                .height(getAdaptiveDp(DefaultTextFieldHeight))
                .weight(1F)
                .focusRequester(focusRequester),
        )

        ElevatedButton(
            onClick = {
                keyboardController?.hide()
                focusManager.clearFocus()
                onAdd()
            },
            enabled = textFieldValue.text.isNotEmpty(),
            contentPadding = PaddingValues(horizontal = 5.dp),
            shape = MaterialTheme.shapes.extraSmall,
            modifier = Modifier
                .height(getAdaptiveDp(DefaultTextFieldHeight)),
        ) {
            AdaptiveIcon(
                painter = painterResource(UiCommonR.drawable.round_add_24),
                contentDescription = stringResource(LocaleR.string.load_url),
            )
        }
    }
}

@Preview
@Composable
private fun SearchBarPreview() {
    val isParseError = remember { mutableStateOf(false) }
    val urlQuery = remember { mutableStateOf("") }

    FlixclusiveTheme {
        Surface {
            AddRepositoryBar(
                urlQuery = { urlQuery.value },
                isParseError = isParseError.value,
                focusRequester = remember { FocusRequester() },
                onUrlQueryChange = { urlQuery.value = it },
                onAdd = {},
                onConsumeError = { isParseError.value = false },
            )
        }
    }
}
