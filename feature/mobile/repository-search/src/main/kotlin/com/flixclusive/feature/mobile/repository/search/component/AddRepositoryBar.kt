package com.flixclusive.feature.mobile.repository.search.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.util.createTextFieldValue
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.feature.mobile.repository.search.util.parseGithubUrl
import com.flixclusive.core.locale.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR

@Composable
internal fun AddRepositoryBar(
    modifier: Modifier = Modifier,
    urlQuery: MutableState<String>,
    isError: MutableState<Boolean>,
    focusRequester: FocusRequester,
    onAdd: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val clipboardManager = LocalClipboardManager.current

    val containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
    val focusedContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(5.dp)

    var textFieldValue by remember {
        val text = clipboardManager.getText()
            ?.text?.let(::parseGithubUrl)
            ?: urlQuery.value

        mutableStateOf(text.createTextFieldValue())
    }


    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
    ) {
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            value = textFieldValue,
            onValueChange = {
                isError.value = false
                textFieldValue = it
                urlQuery.value = it.text
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall,
            keyboardActions = KeyboardActions(
                onGo = {
                    focusManager.clearFocus()
                    keyboardController?.hide()

                    if(urlQuery.value.isEmpty()) {
                        isError.value = true
                        return@KeyboardActions
                    }

                    onAdd()
                }
            ),
            isError = isError.value,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            shape = MaterialTheme.shapes.extraSmall,
            colors = TextFieldDefaults.colors(
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
                    color = LocalContentColor.current.onMediumEmphasis(),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
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
                            urlQuery.value = ""
                            textFieldValue = "".createTextFieldValue()
                        }
                    ) {
                        Icon(
                            painter = painterResource(UiCommonR.drawable.outline_close_square),
                            contentDescription = stringResource(LocaleR.string.clear_text_button)
                        )
                    }
                }
            },
        )

        ElevatedButton(
            onClick = {
                if (urlQuery.value.contains("raw.githubusercontent.com", true)) {
                    val parsedGithubUrl = parseGithubUrl(urlQuery.value)

                    if (parsedGithubUrl == null) {
                        isError.value = true
                        return@ElevatedButton
                    }

                    urlQuery.value = parsedGithubUrl
                }

                keyboardController?.hide()
                focusManager.clearFocus()

                onAdd()
            },
            shape = MaterialTheme.shapes.extraSmall,
            modifier = Modifier
                .height(50.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = stringResource(id = LocaleR.string.load_url),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )
            )
        }
    }
}

@Preview
@Composable
private fun SearchBarPreview() {
    FlixclusiveTheme {
        Surface {
            AddRepositoryBar(
                urlQuery = remember { mutableStateOf("") },
                isError = remember { mutableStateOf(false) },
                focusRequester = remember { FocusRequester() }
            ) {

            }
        }
    }
}