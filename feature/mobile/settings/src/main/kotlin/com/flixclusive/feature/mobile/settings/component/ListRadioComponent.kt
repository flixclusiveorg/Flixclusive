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
import androidx.compose.ui.unit.sp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveStylesUtil.getAdaptiveTextStyle
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveUiUtil.getAdaptiveDp
import com.flixclusive.core.ui.common.util.adaptive.TypographyStyle
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import kotlin.math.max
import com.flixclusive.core.ui.common.R as UiCommonR

@Composable
internal fun <T> ListRadioComponent(
    selectedValue: T,
    title: String,
    options: Map<out T, String>,
    description: String? = null,
    icon: Painter? = null,
    endContent: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    onValueChange: (T) -> Unit,
) {
    var isDialogShown by rememberSaveable { mutableStateOf(false) }

    val onDismissRequest = fun () { isDialogShown = false }

    ClickableComponent(
        modifier = modifier,
        title = title,
        description = description,
        endContent = endContent,
        icon = icon,
        onClick = { isDialogShown = true },
    )

    if (isDialogShown) {
        var selected by rememberSaveable { mutableStateOf(selectedValue) }
        val indexOfSelected = remember {
            options.keys
                .indexOfFirst { it == selected }
        }

        val listState = rememberLazyListState(
            initialFirstVisibleItemIndex = max(indexOfSelected, 0)
        )

        BaseTweakDialog(
            title = title,
            onDismissRequest = onDismissRequest,
            onConfirm = { onValueChange(selected) }
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .heightIn(max = getAdaptiveDp(400.dp, 50.dp))
            ) {
                options.forEach { (option, label) ->
                    val isSelected = selected == option

                    item {
                        Row(
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.small)
                                .selectable(
                                    selected = isSelected,
                                    onClick = { selected = option }
                                )
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { selected = option }
                            )

                            Text(
                                text = label,
                                style = getAdaptiveTextStyle(
                                    style = TypographyStyle.Title
                                ).copy(
                                    fontSize = 16.sp,
                                    fontWeight = if(isSelected) FontWeight.Medium else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.onMediumEmphasis()
                                )
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
    val list = List(20) {
        "Option $it"
    }.associateWith { it }

    FlixclusiveTheme {
        Surface(
            modifier = Modifier.fillMaxSize()
        ) {
            Column {
                ListRadioComponent(
                    title = "List radio tweak with icon",
                    description = "List radio tweak summary",
                    icon = painterResource(UiCommonR.drawable.happy_emphasized),
                    selectedValue = list.keys.last(),
                    options = list,
                    onValueChange = {},
                )

                ListRadioComponent(
                    title = "List radio tweak",
                    description = "List radio tweak summary",
                    selectedValue = list.keys.last(),
                    options = list,
                    onValueChange = {},
                )

                ListRadioComponent(
                    title = "List radio tweak no summary",
                    selectedValue = list.keys.last(),
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