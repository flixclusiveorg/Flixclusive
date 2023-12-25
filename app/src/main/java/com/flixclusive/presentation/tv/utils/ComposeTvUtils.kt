package com.flixclusive.presentation.tv.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.flixclusive.presentation.tv.common.composables.FilmCardShape
import com.flixclusive.presentation.utils.ModifierUtils.ifElse
import com.flixclusive.presentation.utils.FormatterUtils.formatRating

@OptIn(ExperimentalTvMaterial3Api::class)
object ComposeTvUtils {

    data class DirectionalFocusRequester(
        val top: FocusRequester = FocusRequester(),
        val left: FocusRequester = FocusRequester(),
        val bottom: FocusRequester = FocusRequester(),
        val right: FocusRequester = FocusRequester()
    )
    private val LocalDirectionalFocusRequester = compositionLocalOf { DirectionalFocusRequester() }

    @Composable
    fun useLocalDirectionalFocusRequester() = LocalDirectionalFocusRequester.current

    @Composable
    fun provideLocalDirectionalFocusRequester(
        content: @Composable () -> Unit
    ) {
        CompositionLocalProvider(
            value = LocalDirectionalFocusRequester provides remember { DirectionalFocusRequester() },
            content = content
        )
    }

    @Composable
    fun DotSeparatedText(
        modifier: Modifier = Modifier,
        texts: List<String>,
        color: Color = MaterialTheme.colorScheme.onSurface,
        style: TextStyle = MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.Normal
        ),
        contentPadding: Dp = 6.dp
    ) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(contentPadding)
        ) {
            texts.forEachIndexed { index, text ->
                Text(
                    text = text,
                    style = style,
                    color = color
                )

                if (index != texts.lastIndex && texts.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(color)
                            .size(4.dp)
                    )
                }
            }
        }
    }

    @Composable
    fun BoxedEmphasisRating(
        rating: Double,
        containerColor: Color = MaterialTheme.colorScheme.tertiary,
        contentColor: Color = MaterialTheme.colorScheme.onTertiary,
        boxShape: Shape = FilmCardShape
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .graphicsLayer {
                    clip = true
                    shape = boxShape
                }
                .drawBehind {
                    drawRect(color = containerColor)
                }
                .widthIn(
                    min = 25.dp
                )
        ) {
            Text(
                text = formatRating(rating),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = contentColor,
                modifier = Modifier
                    .padding(3.dp)
            )
        }
    }

    @Composable
    fun NonFocusableSpacer(
        width: Dp = Dp.Unspecified,
        height: Dp = Dp.Unspecified
    ) {
        Spacer(
            modifier = Modifier
                .focusProperties {
                    canFocus = false
                }
                .ifElse(
                    width != Dp.Unspecified,
                    Modifier.width(width)
                )
                .ifElse(
                    height != Dp.Unspecified,
                    Modifier.height(height)
                )
        )
    }

    @Composable
    fun colorOnMediumEmphasisTv(
        color: Color = MaterialTheme.colorScheme.onSurface,
        emphasis: Float = 0.6F
    ): Color {
        return color.copy(emphasis)
    }

    fun hasPressedLeft(keyEvent: KeyEvent): Boolean {
        return keyEvent.key == Key.DirectionLeft && keyEvent.type == KeyEventType.KeyUp
    }
}