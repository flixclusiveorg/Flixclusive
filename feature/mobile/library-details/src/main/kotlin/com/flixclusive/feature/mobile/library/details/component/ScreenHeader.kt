package com.flixclusive.feature.mobile.library.details.component

import android.content.res.Resources
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.flixclusive.core.database.entity.library.LibraryList
import com.flixclusive.core.presentation.common.extensions.buildImageRequest
import com.flixclusive.core.presentation.mobile.components.ImageWithSmallPlaceholder
import com.flixclusive.core.presentation.mobile.util.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.feature.mobile.library.details.R
import com.flixclusive.model.provider.ProviderMetadata
import java.util.Date
import com.flixclusive.core.drawables.R as UiCommonR

@Composable
internal fun ScreenHeader(
    library: LibraryList,
    modifier: Modifier = Modifier,
    tracker: ProviderMetadata? = null,
) {
    val time = remember(library) { library.updatedAt.time }
    val resources = LocalResources.current

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        Text(
            text = library.name,
            style = MaterialTheme.typography.headlineMedium
        )

        library.description?.let { description ->
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = LocalContentColor.current.copy(0.6f),
            )
        }

        if (time > 0L) {
            MetadataItem(
                label = stringResource(R.string.edited),
                value = library.updatedAt.toRelativeTimeString(resources),
            )
        }

        tracker?.let {
            Tracker(provider = it)
        }
    }
}

@Composable
private fun Tracker(
    provider: ProviderMetadata,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(3.dp),
    ) {
        ImageWithSmallPlaceholder(
            model = context.buildImageRequest(provider.iconUrl),
            placeholder = painterResource(UiCommonR.drawable.movie_icon),
            contentDescription = provider.name,
            placeholderSize = 12.dp,
            shape = MaterialTheme.shapes.extraSmall,
            modifier = Modifier
                .height(20.dp)
                .aspectRatio(1f),
        )

        Text(
            text = provider.name,
            style = MaterialTheme.typography.labelMedium.asAdaptiveTextStyle().let {
                it.copy(letterSpacing = it.letterSpacing * 1.2f)
            },
            fontWeight = FontWeight.Medium,
            color = LocalContentColor.current.copy(0.7f),
            modifier = Modifier
                .padding(start = 4.dp),
        )
    }
}

@Composable
private fun MetadataItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Text(
            text = "$label $value",
            style = MaterialTheme.typography.labelMedium,
            color = LocalContentColor.current.copy(0.4f),
        )
    }
}

private fun Date.toRelativeTimeString(resources: Resources): String {
    val now = System.currentTimeMillis()
    val diff = now - time

    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    fun format(count: Long, pluralRes: Int): String {
        val unit = resources.getQuantityString(pluralRes, count.toInt())
        return resources.getString(R.string.time_ago, count, unit)
    }

    return when {
        seconds < 60 -> resources.getString(R.string.time_just_now)
        minutes < 60 -> format(minutes, R.plurals.time_minutes)
        hours < 24 -> format(hours, R.plurals.time_hours)
        days < 7 -> format(days, R.plurals.time_days)
        days < 30 -> format(days / 7, R.plurals.time_weeks)
        days < 365 -> format(days / 30, R.plurals.time_months)
        else -> format(days / 365, R.plurals.time_years)
    }
}
