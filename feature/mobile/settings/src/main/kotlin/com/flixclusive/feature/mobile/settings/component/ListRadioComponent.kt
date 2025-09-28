package com.flixclusive.feature.mobile.settings.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.presentation.mobile.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.AdaptiveSizeUtil.getAdaptiveDp
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toImmutableMap
import kotlin.math.max
import com.flixclusive.core.drawables.R as UiCommonR

@Composable
internal fun <T> ListRadioComponent(
    selectedValueProvider: () -> T,
    title: String,
    options: ImmutableMap<out T, String>,
    onValueChange: (T) -> Unit,
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
        enabledProvider = enabledProvider,
        endContent = endContent,
        icon = icon,
        onClick = { isDialogShown = true },
    )

    if (isDialogShown) {
        var selected by remember { mutableStateOf(selectedValueProvider()) }
        val indexOfSelected =
            remember {
                options.keys
                    .indexOfFirst { it == selected }
            }

        val listState =
            rememberLazyListState(
                initialFirstVisibleItemIndex = max(indexOfSelected, 0),
            )

        BaseTweakDialog(
            title = title,
            onDismissRequest = onDismissRequest,
            onConfirm = { onValueChange(selected) },
        ) {
            LazyColumn(
                state = listState,
                modifier =
                    Modifier
                        .heightIn(max = getAdaptiveDp(400.dp, 50.dp)),
            ) {
                options.forEach { (option, label) ->
                    val isSelected = selected == option

                    item {
                        Row(
                            modifier =
                            Modifier
                                .clip(MaterialTheme.shapes.small)
                                .selectable(
                                    selected = isSelected,
                                    onClick = { selected = option },
                                )
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { selected = option },
                            )

                            Text(
                                text = label,
                                style = MaterialTheme.typography.titleMedium.asAdaptiveTextStyle(),
                                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                                color = when {
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurface.copy(0.6f)
                                },
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
private fun ListRadioComponentBasePreview() {
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
                ListRadioComponent(
                    title = "List radio tweak with icon",
                    descriptionProvider = { "List radio tweak summary" },
                    icon = painterResource(UiCommonR.drawable.happy_emphasized),
                    selectedValueProvider = { list.keys.last() },
                    options = list,
                    onValueChange = {},
                )

                ListRadioComponent(
                    title = "List radio tweak",
                    descriptionProvider = { "List radio tweak summary" },
                    selectedValueProvider = { list.keys.last() },
                    options = list,
                    onValueChange = {},
                )

                ListRadioComponent(
                    title = "List radio tweak no summary",
                    selectedValueProvider = { list.keys.last() },
                    options = list,
                    onValueChange = {},
                )
            }
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun ListRadioComponentCompactLandscapePreview() {
    ListRadioComponentBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun ListRadioComponentMediumPortraitPreview() {
    ListRadioComponentBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun ListRadioComponentMediumLandscapePreview() {
    ListRadioComponentBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun ListRadioComponentExtendedPortraitPreview() {
    ListRadioComponentBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun ListRadioComponentExtendedLandscapePreview() {
    ListRadioComponentBasePreview()
}
