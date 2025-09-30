package com.flixclusive.feature.mobile.film.component

import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.core.presentation.mobile.util.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.feature.mobile.film.R
import com.flixclusive.model.film.FilmMetadata
import com.flixclusive.model.film.TvShow
import kotlin.math.roundToInt

private val whitespaceLineRegex = Regex("[\\r\\n]{3,}", setOf(RegexOption.MULTILINE))

@OptIn(ExperimentalAnimationGraphicsApi::class)
@Composable
internal fun CollapsibleDescription(
    metadata: FilmMetadata,
    modifier: Modifier = Modifier,
    isCollapsible: Boolean = true,
) {
    val context = LocalContext.current
    var expanded by rememberSaveable { mutableStateOf(!isCollapsible) }

    // This is used to avoid recomposition issues when the description is not actually collapsible
    // but the isCollapsible param is set to true
    // This can happen when the overview is short or there are no casts/producers/networks
    // We want to make sure that the description is not clickable in this case
    var canCollapse by remember { mutableStateOf(isCollapsible) }

    val animProgress by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        label = "OverviewCaretAnimation",
    )

    val description = remember(metadata) { metadata.getDetailedDescription(context).trimEnd() }

    val textStyle = MaterialTheme.typography.bodySmall
        .copy(color = LocalContentColor.current.copy(0.6f))
        .asAdaptiveTextStyle()

    Layout(
        modifier = modifier
            .clipToBounds()
            .clickable(
                enabled = canCollapse,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                expanded = !expanded
            },
        contents = listOf(
            {
                Text(
                    text = "\n\n", // Shows at least 3 lines
                    style = textStyle,
                )
            },
            {
                Text(
                    text = description,
                    style = textStyle,
                )
            },
            {
                SelectionContainer {
                    Text(
                        text = description,
                        style = textStyle,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = if (expanded) Int.MAX_VALUE else 3,
                    )
                }
            },
            {
                val colors = listOf(Color.Transparent, MaterialTheme.colorScheme.surface)
                val contentDesc = if (expanded) {
                    stringResource(R.string.description_collapse)
                } else {
                    stringResource(R.string.description_expand)
                }

                Box(
                    modifier = Modifier.background(Brush.verticalGradient(colors = colors)),
                    contentAlignment = Alignment.Center,
                ) {
                    val image =
                        AnimatedImageVector.animatedVectorResource(R.drawable.anim_caret_down)

                    AdaptiveIcon(
                        painter = rememberAnimatedVectorPainter(image, !expanded),
                        contentDescription = contentDesc,
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.background(
                            Brush.radialGradient(colors = colors.asReversed()),
                        ),
                    )
                }
            },
        ),
    ) { (shrunk, expanded, actual, scrim), constraints ->
        val shrunkHeight = shrunk
            .single()
            .measure(constraints)
            .height

        val expandedHeight = expanded
            .single()
            .measure(constraints)
            .height

        if (expandedHeight <= shrunkHeight && isCollapsible) {
            canCollapse = false
        }

        val heightDelta = expandedHeight - shrunkHeight
        val scrimHeight = 24.dp.roundToPx()

        val actualPlaceable = actual
            .single()
            .measure(constraints)
        val scrimPlaceable = scrim
            .single()
            .measure(Constraints.fixed(width = constraints.maxWidth, height = scrimHeight))

        val currentHeight = shrunkHeight + ((heightDelta + scrimHeight) * animProgress).roundToInt()

        layout(constraints.maxWidth, currentHeight) {
            actualPlaceable.place(0, 0)

            // Only show crim if the description is collapsible
            // or if the expanded description is actually smaller
            // than the shrunk height (to avoid awkward empty spaces)
            if (canCollapse) {
                val scrimY = currentHeight - scrimHeight
                scrimPlaceable.place(0, scrimY)
            }
        }
    }
}

/**
 * Returns a better description to be displayed in [CollapsibleDescription]
 *
 * Follows the following structure:
 * - Overview
 * - Provider used
 * - Cast
 * - Producers / Network
 * */
private fun FilmMetadata.getDetailedDescription(context: Context): String {
    return buildString {
        overview
            .takeIf { !it.isNullOrBlank() }
            ?.replace(whitespaceLineRegex, "\n")
            ?.trimEnd()
            ?.also { appendLine(it) }
            ?: appendLine(context.getString(R.string.default_overview))

        appendLine()
        appendLine()

        cast
            .takeIf { it.isNotEmpty() }
            ?.take(3)
            ?.joinToString(separator = ", ")
            ?.also { appendLine(context.getString(R.string.casts, it)) }

        producers
            .takeIf { it.isNotEmpty() }
            ?.take(3)
            ?.joinToString(separator = ", ") { it.name }
            ?.also { appendLine(context.getString(R.string.production, it)) }

        if (this@getDetailedDescription is TvShow) {
            networks
                .takeIf { it.isNotEmpty() }
                ?.take(3)
                ?.joinToString(separator = ", ") { it.name }
                ?.also { appendLine(context.getString(R.string.network, it)) }
        }
    }
}
