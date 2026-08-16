package com.flixclusive.feature.mobile.settings.screen.system.logcat.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.presentation.common.extensions.toTextFieldValue
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.core.presentation.mobile.components.material3.CustomOutlinedTextField
import com.flixclusive.core.presentation.mobile.components.material3.topbar.ActionButton
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.feature.mobile.settings.R
import com.flixclusive.feature.mobile.settings.component.AnimatedFilterChip
import com.flixclusive.feature.mobile.settings.screen.system.logcat.LogLevel
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

@Composable
internal fun LogcatFilterField(
    initialQuery: String,
    onQueryChange: (String) -> Unit,
    levelFilters: ImmutableSet<LogLevel>,
    onToggleLevelFilter: (LogLevel) -> Unit,
    filterError: String?,
    modifier: Modifier = Modifier,
) {
    // Seeded once; the field owns its text afterwards, so the query never has to be observed here.
    val textFieldValue = remember { mutableStateOf(initialQuery.toTextFieldValue()) }

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        CustomOutlinedTextField(
            value = textFieldValue.value,
            onValueChange = {
                textFieldValue.value = it
                onQueryChange(it.text)
            },
            singleLine = true,
            isError = filterError != null,
            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            leadingIcon = {
                AdaptiveIcon(
                    painter = painterResource(UiCommonR.drawable.filter_list),
                    contentDescription = stringResource(R.string.logcat_filter_label),
                    dp = 18.dp,
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
                            contentDescription = stringResource(LocaleR.string.close),
                            tint = LocalContentColor.current.copy(0.6f),
                        )
                    }
                }
            },
            placeholder = {
                Text(
                    text = stringResource(R.string.logcat_filter_hint),
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = LocalContentColor.current.copy(0.5f),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )
            },
            supportingText = filterError?.let {
                {
                    Text(
                        text = stringResource(R.string.logcat_filter_invalid),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(SELECTABLE_LEVELS, key = { it.name }) { level ->
                AnimatedFilterChip(
                    selected = level in levelFilters,
                    label = level.name.take(1) + level.name.drop(1).lowercase(),
                    onClick = { onToggleLevelFilter(level) },
                )
            }
        }
    }
}

private val SELECTABLE_LEVELS = listOf(
    LogLevel.VERBOSE,
    LogLevel.DEBUG,
    LogLevel.INFO,
    LogLevel.WARN,
    LogLevel.ERROR,
)

@Preview
@Composable
private fun LogcatFilterFieldPreview() {
    FlixclusiveTheme {
        Surface {
            LogcatFilterField(
                initialQuery = "package:mine level:W",
                onQueryChange = {},
                levelFilters = persistentSetOf(LogLevel.ERROR),
                onToggleLevelFilter = {},
                filterError = null,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
    }
}
