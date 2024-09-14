package com.flixclusive.feature.tv.film.component.buttons


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.OutlinedButtonDefaults
import androidx.tv.material3.Text
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.ui.common.util.IconResource
import com.flixclusive.core.ui.common.R as UiCommonR
import com.flixclusive.core.locale.R as LocaleR

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun WatchlistButton(
    modifier: Modifier = Modifier,
    isInWatchlist: Boolean,
    shape: Shape,
    onClick: () -> Unit,
) {
    var isButtonFocused by remember { mutableStateOf(false) }
    val buttonBorder = Border(
        border = BorderStroke(
            width = 2.dp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.onMediumEmphasis(emphasis = 0.4F)
        ),
        shape = shape
    )

    val (icon, labelId) = remember(isInWatchlist, isButtonFocused) {
        return@remember when {
            isInWatchlist && isButtonFocused -> Pair(
                IconResource.fromImageVector(Icons.Rounded.Close),
                LocaleR.string.remove_from_watchlist
            )
            isInWatchlist -> Pair(
                IconResource.fromImageVector(Icons.Rounded.Check),
                LocaleR.string.remove_from_watchlist
            )
            !isInWatchlist -> Pair(
                IconResource.fromDrawableResource(UiCommonR.drawable.round_add_24),
                LocaleR.string.add_to_watchlist
            )
            else -> throw IllegalStateException("Invalid state for watchlist button.")
        }
    }

    OutlinedButton(
        onClick = onClick,
        shape = OutlinedButtonDefaults.shape(shape),
        border = OutlinedButtonDefaults.border(
            border = buttonBorder,
            focusedBorder = Border.None,
            pressedBorder = Border.None
        ),
        modifier = modifier
            .clip(shape)
            .animateContentSize()
            .onFocusChanged {
                isButtonFocused = it.isFocused
            }
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = icon.asPainterResource(),
                contentDescription = null,
                modifier = Modifier
                    .size(16.dp)
            )

            AnimatedVisibility(
                visible = isButtonFocused,
                enter = fadeIn() + slideInHorizontally(),
                exit = slideOutHorizontally() + fadeOut()
            ) {
                Text(
                    text = stringResource(id = labelId),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}