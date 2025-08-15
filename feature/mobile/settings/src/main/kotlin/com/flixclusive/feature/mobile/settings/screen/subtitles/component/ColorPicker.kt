package com.flixclusive.feature.mobile.settings.screen.subtitles.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.flixclusive.core.presentation.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.adaptive.AdaptiveIcon
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveUiUtil.getAdaptiveDp
import com.flixclusive.core.ui.common.util.boxShadow
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.feature.mobile.settings.TweakPaddingHorizontal
import com.flixclusive.feature.mobile.settings.component.TitleDescriptionHeader
import kotlinx.collections.immutable.persistentListOf
import kotlin.math.max
import com.flixclusive.core.strings.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR

internal val availableColors =
    persistentListOf(
        Color.White,
        Color.Black,
        Color.Red,
        Color.Green,
        Color.Blue,
        Color.Yellow,
        Color(0xFFFFA500),
        Color(0xFF800080),
        Color.Cyan,
        Color.Gray,
    )

@Composable
internal fun ColorPicker(
    title: String,
    selectedColor: Int,
    enabledProvider: () -> Boolean,
    colors: List<Color>,
    onPick: (Color) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
) {
    val fontColor = remember { mutableIntStateOf(selectedColor) }
    val verticalPadding = getAdaptiveDp(10.dp)
    val horizontalPadding = getAdaptiveDp(TweakPaddingHorizontal * 2F)

    val initialFirstVisibleItemIndex = remember { max(colors.indexOf(Color(fontColor.intValue)), 0) }
    val listState =
        rememberLazyListState(
            initialFirstVisibleItemIndex = initialFirstVisibleItemIndex,
        )

    Column(
        modifier =
            modifier
                .padding(vertical = verticalPadding)
                .graphicsLayer {
                    alpha = if (enabledProvider()) 1F else 0.6F
                },
    ) {
        TitleDescriptionHeader(
            title = title,
            descriptionProvider = { description ?: "" },
            modifier =
                Modifier
                    .padding(horizontal = horizontalPadding),
        )

        LazyRow(
            modifier = Modifier
                .focusGroup()
                .padding(top = verticalPadding),
            state = listState,
            contentPadding = PaddingValues(horizontal = horizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(25.dp),
            userScrollEnabled = enabledProvider(),
        ) {
            items(items = colors) { color ->
                ColorButton(
                    color = color,
                    enabled = enabledProvider,
                    isSelected = fontColor.intValue == color.toArgb(),
                    onPick = {
                        onPick(color)
                        fontColor.intValue = color.toArgb()
                    },
                )
            }
        }
    }
}

@Composable
private fun ColorButton(
    color: Color,
    enabled: () -> Boolean,
    isSelected: Boolean,
    onPick: () -> Unit,
) {
    val size = getAdaptiveDp(45.dp)
    val shadowColor = MaterialTheme.colorScheme.surface.onMediumEmphasis(0.3F)
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused = interactionSource.collectIsFocusedAsState()
    val scale = animateFloatAsState(targetValue = if (isFocused.value) 1.2f else 1f)

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onPick,
                    enabled = enabled(),
                )
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                },
    ) {
        Box(
            modifier =
                Modifier
                    .size(size)
                    .background(color = color, shape = CircleShape)
                    .boxShadow(
                        color = shadowColor,
                        blurRadius = 4.dp,
                        spreadRadius = 1.dp,
                        offset = DpOffset(3.dp, 4.dp),
                        inset = true,
                        shape = CircleShape,
                    )
                    .indication(
                        interactionSource = interactionSource,
                        indication = LocalIndication.current
                    ),
        )

        AnimatedVisibility(
            visible = isSelected && enabled(),
            enter = scaleIn(),
            exit = scaleOut() + fadeOut(),
            modifier =
                Modifier
                    .align(Alignment.Center),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .border(
                            0.5.dp,
                            shadowColor.copy(0.1F),
                            CircleShape,
                        )
                        .boxShadow(
                            color = shadowColor,
                            blurRadius = 4.dp,
                            spreadRadius = 0.05.dp,
                            offset = DpOffset(3.dp, 3.dp),
                            shape = CircleShape,
                        )
                        .size(size / 2)
                        .background(color = Color.White, shape = CircleShape),
            ) {
                AdaptiveIcon(
                    painter = painterResource(UiCommonR.drawable.check),
                    contentDescription = stringResource(LocaleR.string.selected_label),
                    tint = Color.Black,
                    dp = 18.dp,
                    increaseBy = 4.dp,
                )
            }
        }
    }
}

@Preview
@Composable
private fun ColorPickerBasePreview() {
    var selectedColor by remember { mutableIntStateOf(Color.White.toArgb()) }
    FlixclusiveTheme {
        Surface {
            ColorPicker(
                selectedColor = selectedColor,
                title = "Subtitle",
                enabledProvider = { false },
                colors =
                    listOf(
                        Color.White,
                        Color.Black,
                        Color.Red,
                        Color.Green,
                        Color.Blue,
                        Color.Yellow,
                        Color(0xFFFFA500),
                        Color(0xFF800080),
                        Color.Cyan,
                        Color.Gray,
                    ),
                onPick = { selectedColor = it.toArgb() },
            )
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun ColorPickerCompactLandscapePreview() {
    ColorPickerBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun ColorPickerMediumPortraitPreview() {
    ColorPickerBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun ColorPickerMediumLandscapePreview() {
    ColorPickerBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun ColorPickerExtendedPortraitPreview() {
    ColorPickerBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun ColorPickerExtendedLandscapePreview() {
    ColorPickerBasePreview()
}
