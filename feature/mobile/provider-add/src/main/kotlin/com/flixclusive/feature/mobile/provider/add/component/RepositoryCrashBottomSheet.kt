package com.flixclusive.feature.mobile.provider.add.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.presentation.common.extensions.showToast
import com.flixclusive.core.presentation.common.util.CustomClipboardManager.Companion.rememberClipboardManager
import com.flixclusive.core.presentation.mobile.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.core.presentation.mobile.components.CommonBottomSheet
import com.flixclusive.core.presentation.mobile.components.Placeholder
import com.flixclusive.core.presentation.mobile.extensions.isCompact
import com.flixclusive.core.presentation.mobile.extensions.isExpanded
import com.flixclusive.core.presentation.mobile.extensions.isMedium
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.getFeedbackOnLongPress
import com.flixclusive.feature.mobile.provider.add.R
import com.flixclusive.feature.mobile.provider.add.RepositoryWithError
import com.flixclusive.model.provider.Repository
import com.flixclusive.core.drawables.R as UiCommonR

@Composable
internal fun RepositoryCrashBottomSheet(
    isLoading: Boolean,
    errors: List<RepositoryWithError>,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val windowWidthSizeClass = windowSizeClass.windowWidthSizeClass

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val maxWidth = when {
        windowWidthSizeClass.isMedium -> screenWidth / 2.5f
        windowWidthSizeClass.isExpanded -> screenWidth / 3
        else -> screenWidth
    }

    CommonBottomSheet(onDismissRequest = onDismissRequest, modifier = modifier) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(maxWidth),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                LabelHeader(
                    errorCount = errors.size,
                    modifier = Modifier
                        .padding(bottom = 16.dp),
                )
            }

            itemsIndexed(errors) { i, (repository, error) ->
                Column {
                    RepositoryCrashItem(
                        repository = repository,
                        error = error,
                    )

                    if (i < errors.size && windowWidthSizeClass.isCompact) {
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = LocalContentColor.current.copy(0.4f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 15.dp),
                        )
                    } else if (!windowWidthSizeClass.isCompact) {
                        Spacer(modifier = Modifier.padding(vertical = 15.dp))
                    }
                }
            }

            if (isLoading) {
                items(2) {
                    RepositoryCrashItemPlaceholder()
                }
            }
        }
    }
}

@Composable
private fun LabelHeader(
    errorCount: Int,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AdaptiveIcon(
                painter = painterResource(UiCommonR.drawable.warning_outline),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                dp = 24.dp,
            )

            Text(
                text = stringResource(R.string.repository_failure),
                style = MaterialTheme.typography.titleLarge.asAdaptiveTextStyle(),
                color = MaterialTheme.colorScheme.error,
            )
        }

        Text(
            text = context.resources.getQuantityString(R.plurals.repository_failure_description, errorCount),
            style = MaterialTheme.typography.bodySmall.asAdaptiveTextStyle(11.sp),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RepositoryCrashItem(
    repository: Repository,
    error: UiText,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    val hapticFeedback = getFeedbackOnLongPress()
    val clipboardManager = rememberClipboardManager()

    Column(
        verticalArrangement = Arrangement.spacedBy(15.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraSmall)
            .combinedClickable(
                onClick = { uriHandler.openUri(repository.url) },
                onLongClick = {
                    hapticFeedback()
                    clipboardManager.setText(repository.url)
                    context.showToast("${repository.name}'s URL copied to clipboard")
                },
            ),
    ) {
        Column {
            Text(
                text = repository.name,
                fontWeight = FontWeight.Bold,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelLarge.asAdaptiveTextStyle(),
            )

            Text(
                text = repository.owner,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelMedium.asAdaptiveTextStyle(),
                color = LocalContentColor.current.copy(0.8f),
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        Text(
            text = error.asString(),
            style = MaterialTheme.typography.bodySmall.asAdaptiveTextStyle(10.sp),
            overflow = TextOverflow.Ellipsis,
            color = LocalContentColor.current.copy(0.8f),
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
private fun RepositoryCrashItemPlaceholder(modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(15.dp),
        modifier = modifier,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Placeholder(
                modifier = Modifier
                    .height(12.dp)
                    .fillMaxWidth(0.25f),
            )

            Placeholder(
                modifier = Modifier
                    .height(10.dp)
                    .fillMaxWidth(0.6f),
            )
        }

        Placeholder(
            modifier = Modifier
                .height(12.dp)
                .fillMaxWidth(),
        )
    }
}

@Preview
@Composable
private fun RepositoryCrashBottomSheetBasePreview() {
    val errors = remember {
        List(10) {
            Repository(
                name = "Example Repo $it",
                owner = "Example Owner $it",
                rawLinkFormat = "https://example.com/$it",
                url = "https://example.com/$it",
            ) to UiText.from("Example error message $it")
        }
    }

    FlixclusiveTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            RepositoryCrashBottomSheet(
                isLoading = true,
                errors = errors,
                onDismissRequest = {},
            )
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun RepositoryCrashBottomSheetCompactLandscapePreview() {
    RepositoryCrashBottomSheetBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun RepositoryCrashBottomSheetMediumPortraitPreview() {
    RepositoryCrashBottomSheetBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun RepositoryCrashBottomSheetMediumLandscapePreview() {
    RepositoryCrashBottomSheetBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun RepositoryCrashBottomSheetExtendedPortraitPreview() {
    RepositoryCrashBottomSheetBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun RepositoryCrashBottomSheetExtendedLandscapePreview() {
    RepositoryCrashBottomSheetBasePreview()
}
