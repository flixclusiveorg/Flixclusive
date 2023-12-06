package com.flixclusive.presentation.tv.screens.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceColors
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ClickableSurfaceShape
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.flixclusive.R
import com.flixclusive.presentation.tv.utils.ComposeTvUtils.colorOnMediumEmphasisTv
import com.flixclusive.presentation.tv.utils.ModifierTvUtils.focusOnInitialVisibility
import com.flixclusive.presentation.utils.ModifierUtils.ifElse

val KeyboardCellSize = 35.dp

data class ButtonSize(
    val width: Dp,
    val height: Dp
) {
    constructor(size: Dp) : this(width = size, height = size)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchCustomKeyboard(
    currentSearchQuery: String,
    onKeyboardClick: (letter: Char) -> Unit,
    onBackspacePress: () -> Unit
) {
    var areSymbolsShown by remember { mutableStateOf(false) }
    val symbolButtonText = remember(areSymbolsShown) {
        if(!areSymbolsShown) {
            "&128"
        } else {
            "ABC"
        }
    }

    val symbolsAndNumbers = listOf(
        '1', '2', '3', '&', '#', '(', ')',
        '4', '5', '6', '@', '!', '?', ':',
        '7', '8', '9', '.', '-', '_', '"',
        '0', '/', '$', '%', '+', '[', ']'
    )
    val alphabets = ('a'..'z').toList() + listOf('-', '\'')
    val isFirstItemVisible = remember { mutableStateOf(false) }

    val textStyle = MaterialTheme.typography.bodyMedium.copy(
        fontSize = 16.sp
    )

    val longButtonSize = ButtonSize(
        height = KeyboardCellSize,
        width = 85.dp
    )
    val longButtonColors = ClickableSurfaceDefaults.colors(
        containerColor = colorOnMediumEmphasisTv(emphasis = 0.2F),
        contentColor = MaterialTheme.colorScheme.onSurface,
        focusedContainerColor = Color.White,
        focusedContentColor = MaterialTheme.colorScheme.surface
    )
    val longButtonShape = ClickableSurfaceDefaults.shape(
        shape = MaterialTheme.shapes.extraSmall
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        AnimatedVisibility(
            visible = !areSymbolsShown,
            enter = fadeIn(animationSpec = tween(delayMillis = 100)),
            exit = fadeOut(animationSpec = tween(delayMillis = 100))
        ) {
            FlowRow {
                alphabets.forEachIndexed { i, alphabet ->
                    KeyboardButton(
                        onClick = {
                            onKeyboardClick(alphabet)
                        },
                        modifier = Modifier
                            .ifElse(
                                condition = i == 0 && !isFirstItemVisible.value,
                                ifTrueModifier = Modifier.focusOnInitialVisibility(isFirstItemVisible)
                            )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = alphabet.uppercase(),
                                style = textStyle
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = areSymbolsShown,
            enter = fadeIn(animationSpec = tween(delayMillis = 100)),
            exit = fadeOut(animationSpec = tween(delayMillis = 100))
        ) {
            FlowRow {
                symbolsAndNumbers.forEachIndexed { i, symbolOrNumber ->
                    KeyboardButton(
                        onClick = {
                            onKeyboardClick(symbolOrNumber)
                        },
                        modifier = Modifier
                            .ifElse(
                                condition = i == 0 && !isFirstItemVisible.value,
                                ifTrueModifier = Modifier.focusOnInitialVisibility(isFirstItemVisible)
                            )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = symbolOrNumber.toString(),
                                style = textStyle
                            )
                        }
                    }
                }
            }
        }
        
        FlowRow {
            KeyboardButton(
                onClick = {
                    onKeyboardClick(' ')
                },
                size = longButtonSize,
                colors = longButtonColors,
                shape = longButtonShape
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.round_space_bar_24),
                        contentDescription = null
                    )
                }
            }

            KeyboardButton(
                onClick = {
                    areSymbolsShown = !areSymbolsShown
                },
                size = longButtonSize,
                colors = longButtonColors,
                shape = longButtonShape
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = symbolButtonText.uppercase(),
                        style = textStyle.copy(
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            KeyboardButton(
                onClick = onBackspacePress,
                enabled = currentSearchQuery.isNotEmpty(),
                size = longButtonSize,
                colors = longButtonColors,
                shape = longButtonShape
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.round_backspace_24),
                        contentDescription = null
                    )
                }
            }
        }
    }
}

@Composable
fun KeyboardButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: ButtonSize = ButtonSize(KeyboardCellSize),
    colors: ClickableSurfaceColors = ClickableSurfaceDefaults.colors(
        containerColor = Color.Transparent,
        contentColor = colorOnMediumEmphasisTv(),
        focusedContainerColor = MaterialTheme.colorScheme.inverseSurface,
        focusedContentColor = MaterialTheme.colorScheme.inverseOnSurface
    ),
    shape: ClickableSurfaceShape = ClickableSurfaceDefaults.shape(
        shape = RectangleShape
    ),
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Surface(
        enabled = enabled,
        onClick = onClick,
        colors = colors,
        shape = shape,
        modifier = Modifier
            .size(size.width, size.height)
            .padding(3.dp)
            .then(modifier)
    ) {
        content()
    }
}