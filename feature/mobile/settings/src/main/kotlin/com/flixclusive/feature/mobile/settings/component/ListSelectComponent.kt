package com.flixclusive.feature.mobile.settings.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.presentation.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveStylesUtil.getAdaptiveTextStyle
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveUiUtil.getAdaptiveDp
import com.flixclusive.core.ui.common.util.adaptive.TypographyStyle
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.presentation.mobile.components.LabeledCheckbox
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toImmutableMap
import kotlin.math.max
import com.flixclusive.core.ui.common.R as UiCommonR

@Composable
internal fun <T> ListSelectComponent(
    selectedValuesProvider: () -> Set<T>,
    title: String,
    options: ImmutableMap<out T, String>,
    onValueChange: (Set<T>) -> Unit,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    descriptionProvider: (() -> String)? = null,
    enabledProvider: () -> Boolean = { true },
    endContent: @Composable (() -> Unit)? = null,
) {
    var isDialogShown by rememberSaveable { mutableStateOf(false) }

    val onDismissRequest = fun() {
        isDialogShown = false
    }

    ClickableComponent(
        modifier = modifier,
        title = title,
        descriptionProvider = descriptionProvider,
        endContent = endContent,
        enabledProvider = enabledProvider,
        icon = icon,
        onClick = { isDialogShown = true },
    )

    if (isDialogShown) {
        val selectedValuesProviderUpdated by rememberUpdatedState(selectedValuesProvider)
        val selected by remember {
            derivedStateOf {
                options.keys
                    .filter { selectedValuesProviderUpdated().contains(it) }
                    .toMutableStateList()
            }
        }

        val indexOfSelected =
            remember {
                options.keys
                    .indexOfFirst { it == selected.firstOrNull() }
            }

        val listState =
            rememberLazyListState(
                initialFirstVisibleItemIndex = max(indexOfSelected, 0),
            )

        val onSelect = fun(
            isAdding: Boolean,
            option: T,
        ) {
            when {
                isAdding -> selected.add(option)
                else -> selected.remove(option)
            }
        }

        BaseTweakDialog(
            title = title,
            onDismissRequest = onDismissRequest,
            onConfirm = { onValueChange(selected.toSet()) },
        ) {
            LazyColumn(
                state = listState,
                modifier =
                    Modifier
                        .heightIn(max = getAdaptiveDp(400.dp, 50.dp)),
            ) {
                options.forEach { (option, label) ->
                    val isSelected = selected.contains(option)

                    item {
                        LabeledCheckbox(
                            checked = isSelected,
                            onCheckedChange = { onSelect(it, option) },
                            modifier =
                                Modifier
                                    .clip(MaterialTheme.shapes.small)
                                    .clickable(onClick = { onSelect(!isSelected, option) })
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .padding(horizontal = getAdaptiveDp(10.dp)),
                        ) {
                            Text(
                                text = label,
                                style =
                                    getAdaptiveTextStyle(
                                        style = TypographyStyle.Title,
                                    ).copy(
                                        fontSize = 16.sp,
                                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                                        color =
                                            if (isSelected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurface.copy(0.6f)
                                            },
                                    ),
                                modifier =
                                    Modifier
                                        .padding(start = getAdaptiveDp(10.dp)),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun ListSelectComponentBasePreview() {
    var selected by remember { mutableStateOf(setOf<String>()) }
    val list =
        List(20) {
            "Option $it"
        }.associateWith { it }
            .toImmutableMap()

    FlixclusiveTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
        ) {
            Column {
                ListSelectComponent(
                    title = "List select tweak with icon",
                    descriptionProvider = { "List select tweak summary" },
                    icon = painterResource(UiCommonR.drawable.happy_emphasized),
                    selectedValuesProvider = { selected },
                    options = list,
                    onValueChange = { selected = it },
                )

                ListSelectComponent(
                    title = "List select tweak",
                    descriptionProvider = { "List select tweak summary" },
                    selectedValuesProvider = { selected.toSet() },
                    options = list,
                    onValueChange = { selected = it },
                )

                ListSelectComponent(
                    title = "List select tweak no summary",
                    selectedValuesProvider = { selected.toSet() },
                    options = list,
                    onValueChange = { selected = it },
                )
            }
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun ListSelectComponentCompactLandscapePreview() {
    ListSelectComponentBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun ListSelectComponentMediumPortraitPreview() {
    ListSelectComponentBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun ListSelectComponentMediumLandscapePreview() {
    ListSelectComponentBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun ListSelectComponentExtendedPortraitPreview() {
    ListSelectComponentBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun ListSelectComponentExtendedLandscapePreview() {
    ListSelectComponentBasePreview()
}
