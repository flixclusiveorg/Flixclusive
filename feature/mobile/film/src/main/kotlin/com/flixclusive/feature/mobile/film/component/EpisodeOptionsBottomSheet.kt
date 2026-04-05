package com.flixclusive.feature.mobile.film.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.core.presentation.mobile.components.material3.CommonBottomSheet
import com.flixclusive.core.presentation.mobile.util.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.domain.provider.model.EpisodeWithProgress
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

@Composable
internal fun EpisodeOptionsBottomSheet(
    episode: EpisodeWithProgress,
    onToggleWatchStatus: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    val isWatched = episode.watchProgress?.isCompleted == true

    val label = if (isWatched) {
        stringResource(LocaleR.string.mark_as_unwatched)
    } else {
        stringResource(LocaleR.string.mark_as_watched)
    }

    val icon = if (isWatched) {
        painterResource(UiCommonR.drawable.round_close_24)
    } else {
        painterResource(UiCommonR.drawable.check)
    }

    CommonBottomSheet(onDismissRequest = onDismissRequest) {
        TextButton(
            onClick = onToggleWatchStatus,
            shape = MaterialTheme.shapes.small,
            contentPadding = PaddingValues(horizontal = 16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            AdaptiveIcon(
                painter = icon,
                contentDescription = label,
            )

            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.asAdaptiveTextStyle(),
                fontWeight = FontWeight.Bold,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                modifier = Modifier
                    .padding(start = 16.dp)
                    .weight(1f),
            )
        }
    }
}
