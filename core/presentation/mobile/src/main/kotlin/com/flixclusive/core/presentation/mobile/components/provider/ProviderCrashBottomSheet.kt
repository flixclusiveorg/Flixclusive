package com.flixclusive.core.presentation.mobile.components.provider

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
import androidx.compose.foundation.lazy.grid.itemsIndexed
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
import com.flixclusive.core.common.provider.ProviderWithThrowable
import com.flixclusive.core.common.provider.extensions.toOwnerAndRepository
import com.flixclusive.core.presentation.common.extensions.buildImageRequest
import com.flixclusive.core.presentation.common.util.DummyDataForPreview
import com.flixclusive.core.presentation.mobile.R
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.core.presentation.mobile.components.ImageWithSmallPlaceholder
import com.flixclusive.core.presentation.mobile.components.Placeholder
import com.flixclusive.core.presentation.mobile.components.material3.CommonBottomSheet
import com.flixclusive.core.presentation.mobile.extensions.isCompact
import com.flixclusive.core.presentation.mobile.extensions.isExpanded
import com.flixclusive.core.presentation.mobile.extensions.isMedium
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.theme.MobileColors.surfaceColorAtElevation
import com.flixclusive.core.presentation.mobile.util.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.core.drawables.R as UiCommonR

@Composable
fun ProviderCrashBottomSheet(
    isLoading: Boolean,
    errors: List<ProviderWithThrowable>,
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

    val onDismiss by rememberUpdatedState(onDismissRequest)
    var detailedCrashLog by remember { mutableStateOf<ProviderWithThrowable?>(null) }

    if (detailedCrashLog != null) {
        val (provider, error) = detailedCrashLog!!

        ProviderCrashDialog(
            provider = provider,
            error = error,
            onDismissRequest = { detailedCrashLog = null },
        )
    }

    CommonBottomSheet(onDismissRequest = onDismiss, modifier = modifier) {
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

            itemsIndexed(
                errors,
                key = { _, (provider) -> provider.id },
            ) { i, error ->
                Column(
                    modifier = Modifier.animateItem(),
                ) {
                    CrashItem(
                        provider = error.provider,
                        error = error.throwable,
                        onClick = { detailedCrashLog = error },
                    )

                    if (i < errors.lastIndex && windowWidthSizeClass.isCompact) {
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
private fun LabelHeader(
    errorCount: Int,
    modifier: Modifier = Modifier
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
                stringResource(R.string.provider_failure),
                style = MaterialTheme.typography.titleLarge.asAdaptiveTextStyle(),
                color = MaterialTheme.colorScheme.error,
            )
        }

        Text(
            text = context.resources.getQuantityString(R.plurals.provider_failure_sub_text, errorCount),
            style = MaterialTheme.typography.bodySmall.asAdaptiveTextStyle(11.sp),
        )
    }
}

@Composable
private fun CrashItem(
    provider: ProviderMetadata,
    error: Throwable,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(15.dp),
        modifier = modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .clickable { onClick() },
    ) {
        CrashItemTopContent(provider = provider)

        StackTracePreview(error = remember { error.stackTraceToString() })
    }
}

@Composable
internal fun CrashItemTopContent(
    provider: ProviderMetadata,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    val providerLogo = remember(provider) {
        context.buildImageRequest(provider.iconUrl)
    }

    val repository = remember(provider) {
        val pair = provider.repositoryUrl.toOwnerAndRepository()
        requireNotNull(pair) {
            "Could not extract github info from link: ${provider.repositoryUrl}"
        }

        val (username, repository) = pair

        "$username/$repository"
    }

    val version = "${provider.versionName} (${provider.versionCode}) - ${provider.id}"

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ImageWithSmallPlaceholder(
            model = providerLogo,
            placeholder = painterResource(UiCommonR.drawable.provider_logo),
            contentDescription = provider.name,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.size(65.dp),
        )

        Column {
            Text(
                text = provider.name,
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
                    .clickable { uriHandler.openUri(provider.repositoryUrl) }
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
            )
            .background(
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
            val provider = DummyDataForPreview.getDummyProviderMetadata(
                id = it.toString(),
                name = "Provider $it",
            )
            val error = NullPointerException("This is a sample error message for provider $it.")

            ProviderWithThrowable(
                provider = provider,
                throwable = error,
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
