package com.flixclusive.feature.mobile.profiles

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
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
import com.flixclusive.core.presentation.common.extensions.buildImageRequest
import com.flixclusive.core.presentation.common.util.DummyDataForPreview
import com.flixclusive.core.presentation.mobile.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.core.presentation.mobile.components.CommonBottomSheet
import com.flixclusive.core.presentation.mobile.components.ImageWithSmallPlaceholder
import com.flixclusive.core.presentation.mobile.components.Placeholder
import com.flixclusive.core.presentation.mobile.extensions.isCompact
import com.flixclusive.core.presentation.mobile.extensions.isExpanded
import com.flixclusive.core.presentation.mobile.extensions.isMedium
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.theme.MobileColors.surfaceColorAtElevation
import com.flixclusive.domain.provider.usecase.manage.LoadProviderResult
import com.flixclusive.domain.provider.util.extractGithubInfoFromLink
import com.flixclusive.core.drawables.R as UiCommonR

@Composable
internal fun ProviderCrashBottomSheet(
    isLoading: Boolean,
    errors: Collection<LoadProviderResult.Failure>,
    onDismissRequest: () -> Unit,
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

    val onDismiss by rememberUpdatedState(onDismissRequest)
    var detailedCrashLog by remember { mutableStateOf<LoadProviderResult.Failure?>(null) }

    if (detailedCrashLog != null) {
        ProviderCrashDialog(
            error = detailedCrashLog!!,
            onDismissRequest = { detailedCrashLog = null },
        )
    }

    CommonBottomSheet(onDismissRequest = onDismiss) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(maxWidth),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                LabelHeader(
                    modifier = Modifier
                        .padding(bottom = 16.dp),
                )
            }

            items(
                errors.size,
                key = { index -> errors.elementAt(index).provider.id },
            ) { i ->
                val error = errors.elementAt(i)

                Column(
                    modifier = Modifier.animateItem(),
                ) {
                    CrashItem(
                        error = error,
                        onClick = { detailedCrashLog = error },
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
                    CrashItemPlaceholder(
                        modifier = Modifier.padding(vertical = 15.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun LabelHeader(modifier: Modifier = Modifier) {
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
                stringResource(R.string.provider_failure),
                style = MaterialTheme.typography.titleLarge.asAdaptiveTextStyle(),
                color = MaterialTheme.colorScheme.error,
            )
        }

        Text(
            text = stringResource(R.string.provider_failure_sub_text),
            style = MaterialTheme.typography.bodySmall.asAdaptiveTextStyle(11.sp),
        )
    }
}

@Composable
private fun CrashItem(
    error: LoadProviderResult.Failure,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(15.dp),
        modifier = modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .clickable { onClick() },
    ) {
        CrashItemTopContent(error = error)

        StackTracePreview(error = remember { error.error.stackTraceToString() })
    }
}

@Composable
internal fun CrashItemTopContent(
    error: LoadProviderResult.Failure,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    val providerLogo = remember(error.provider) {
        context.buildImageRequest(error.provider.iconUrl)
    }

    val repository = remember(error.provider) {
        val pair = extractGithubInfoFromLink(error.provider.repositoryUrl)
        requireNotNull(pair) {
            "Could not extract github info from link: ${error.provider.repositoryUrl}"
        }

        val (username, repository) = pair

        "$username/$repository"
    }

    val version = "${error.provider.versionName} (${error.provider.versionCode}) - ${error.provider.id}"

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ImageWithSmallPlaceholder(
            model = providerLogo,
            placeholder = painterResource(UiCommonR.drawable.provider_logo),
            contentDescription = error.provider.name,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.size(65.dp),
        )

        Column {
            Text(
                text = error.provider.name,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelLarge.asAdaptiveTextStyle(),
            )

            Text(
                text = repository,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelMedium.asAdaptiveTextStyle(),
                color = LocalContentColor.current.copy(0.6f),
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .clickable { uriHandler.openUri(error.provider.repositoryUrl) }
                    .padding(vertical = 1.dp),
            )

            Text(
                text = version,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelMedium.asAdaptiveTextStyle(),
                color = LocalContentColor.current.copy(0.8f),
            )
        }
    }
}

@Composable
private fun CrashItemPlaceholder(modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(15.dp),
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Placeholder(
                modifier = Modifier.size(65.dp),
            )

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

                Placeholder(
                    modifier = Modifier
                        .height(11.dp)
                        .fillMaxWidth(0.55f),
                )
            }
        }

        Placeholder(
            modifier = Modifier
                .height(20.dp)
                .fillMaxWidth(),
        )
    }
}

@Composable
private fun StackTracePreview(
    error: String,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 0.5.dp,
                color = LocalContentColor.current.copy(0.6f),
                shape = MaterialTheme.shapes.extraSmall,
            ).background(
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(1),
                shape = MaterialTheme.shapes.extraSmall,
            ),
    ) {
        Text(
            text = error,
            style = MaterialTheme.typography.bodySmall.asAdaptiveTextStyle(10.sp),
            modifier = Modifier.padding(4.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = LocalContentColor.current.copy(0.8f),
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Preview
@Composable
private fun ProviderCrashBottomSheetBasePreview() {
    val errors = remember {
        List(10) {
            LoadProviderResult.Failure(
                provider = DummyDataForPreview.getDummyProviderMetadata(
                    id = it.toString(),
                    name = "Provider $it",
                ),
                error = NullPointerException("This is a sample error message for provider $it."),
                filePath = "SampleFilePath.kt:42",
            )
        }
    }

    FlixclusiveTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
        ) {
            ProviderCrashBottomSheet(
                isLoading = true,
                errors = errors,
                onDismissRequest = {},
            )
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun ProviderCrashBottomSheetCompactLandscapePreview() {
    ProviderCrashBottomSheetBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun ProviderCrashBottomSheetMediumPortraitPreview() {
    ProviderCrashBottomSheetBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun ProviderCrashBottomSheetMediumLandscapePreview() {
    ProviderCrashBottomSheetBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun ProviderCrashBottomSheetExtendedPortraitPreview() {
    ProviderCrashBottomSheetBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun ProviderCrashBottomSheetExtendedLandscapePreview() {
    ProviderCrashBottomSheetBasePreview()
}
