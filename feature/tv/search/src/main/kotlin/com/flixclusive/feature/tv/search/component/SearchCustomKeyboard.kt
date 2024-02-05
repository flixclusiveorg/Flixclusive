package com.flixclusive.feature.tv.search.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.flixclusive.feature.tv.search.R

internal val KeyboardCellSize = 35.dp
internal const val KEYBOARD_FOCUS_KEY_FORMAT = "keyboard=%s"

internal data class ButtonSize(
    val width: Dp,
    val height: Dp
) {
    constructor(size: Dp) : this(width = size, height = size)
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalTvMaterial3Api::class)
@Composable
internal fun SearchCustomKeyboard(
    modifier: Modifier = Modifier,
    currentSearchQuery: String,
    onKeyboardClick: (letter: Char) -> Unit,
    onBackspaceClick: () -> Unit,
    onBackspaceLongClick: () -> Unit,
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

    val textStyle = MaterialTheme.typography.bodyMedium.copy(
        fontSize = 16.sp
    )

    val longButtonSize = ButtonSize(
        height = KeyboardCellSize,
        width = (KeyboardCellSize * 2) + 6.dp
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(5.dp),
        modifier = modifier
    ) {
        Box {
            this@Column.AnimatedVisibility(
                visible = !areSymbolsShown,
                enter = fadeIn(animationSpec = tween(delayMillis = 100)),
                exit = fadeOut(animationSpec = tween(delayMillis = 100))
            ) {
                FlowRow(
                    verticalArrangement = Arrangement.Center,
                    horizontalArrangement = Arrangement.Center
                ) {
                    alphabets.forEach {
                        KeyboardButton(
                            onClick = { onKeyboardClick(it) },
                            itemKey = String.format(KEYBOARD_FOCUS_KEY_FORMAT, it)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = it.uppercase(),
                                    style = textStyle
                                )
                            }
                        }
                    }
                }
            }

            this@Column.AnimatedVisibility(
                visible = areSymbolsShown,
                enter = fadeIn(animationSpec = tween(delayMillis = 100)),
                exit = fadeOut(animationSpec = tween(delayMillis = 100))
            ) {
                FlowRow(
                    verticalArrangement = Arrangement.Center,
                    horizontalArrangement = Arrangement.Center
                ) {
                    symbolsAndNumbers.forEach {
                        KeyboardButton(
                            onClick = { onKeyboardClick(it) },
                            itemKey = String.format(KEYBOARD_FOCUS_KEY_FORMAT, it)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = it.toString(),
                                    style = textStyle
                                )
                            }
                        }
                    }
                }
            }
        }
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            KeyboardButton(
                onClick = {
                    onKeyboardClick(' ')
                },
                size = longButtonSize,
                itemKey = String.format(KEYBOARD_FOCUS_KEY_FORMAT, "space")
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
                itemKey = String.format(KEYBOARD_FOCUS_KEY_FORMAT, "symbols")
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
                onClick = onBackspaceClick,
                enabled = currentSearchQuery.isNotEmpty(),
                size = longButtonSize,
                itemKey = String.format(KEYBOARD_FOCUS_KEY_FORMAT, "backpress"),
                onLongClick = onBackspaceLongClick
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