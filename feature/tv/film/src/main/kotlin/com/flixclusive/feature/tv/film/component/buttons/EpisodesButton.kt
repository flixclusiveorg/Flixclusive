package com.flixclusive.feature.tv.film.component.buttons

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
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
import com.flixclusive.core.ui.player.R as PlayerR
import com.flixclusive.core.locale.R as LocaleR

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun EpisodesButton(
    modifier: Modifier = Modifier,
    shape: Shape,
    onClick: () -> Unit,
) {
    val buttonBorder = Border(
        border = BorderStroke(
            width = 2.dp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.onMediumEmphasis(emphasis = 0.4F)
        ),
        shape = shape
    )

    OutlinedButton(
        onClick = onClick,
        border = OutlinedButtonDefaults.border(
            border = buttonBorder,
            focusedBorder = buttonBorder,
            pressedBorder = buttonBorder
        ),
        shape = OutlinedButtonDefaults.shape(shape),
        modifier = modifier
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = PlayerR.drawable.outline_video_library_24),
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )

            Text(
                text = stringResource(id = LocaleR.string.episodes),
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