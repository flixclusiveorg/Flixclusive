package com.flixclusive.feature.mobile.home.components

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.common.domain.Async.Companion.AsyncAnimatedContent
import com.flixclusive.core.common.provider.extensions.asStatusColor
import com.flixclusive.core.presentation.common.extensions.buildImageRequest
import com.flixclusive.core.presentation.common.extensions.fadingEdge
import com.flixclusive.core.presentation.common.theme.Elevations
import com.flixclusive.core.presentation.common.util.DummyDataForPreview
import com.flixclusive.core.presentation.mobile.components.EmptyDataMessage
import com.flixclusive.core.presentation.mobile.components.ImageWithSmallPlaceholder
import com.flixclusive.core.presentation.mobile.components.Placeholder
import com.flixclusive.core.presentation.mobile.components.material3.CommonBottomSheet
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.feature.mobile.home.CatalogProvider
import com.flixclusive.feature.mobile.home.R
import com.flixclusive.model.provider.ProviderStatus
import kotlinx.coroutines.launch
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

@Composable
internal fun CatalogProvidersBottomSheet(
    providers: Async<List<CatalogProvider>>,
    onDismiss: () -> Unit,
    onToggle: (CatalogProvider) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    CommonBottomSheet(onDismiss) {
        Text(
            text = stringResource(R.string.catalog_providers_sheet_label),
            style = MaterialTheme.typography.titleMedium,
        )

        Text(
            text = stringResource(R.string.catalog_providers_sheet_desc),
            style = MaterialTheme.typography.bodySmall,
            color = LocalContentColor.current.copy(0.7f),
            modifier = Modifier.padding(vertical = 4.dp)
        )

        AsyncAnimatedContent(
            targetState = providers,
            modifier = modifier.padding(vertical = 12.dp),
            loadingContent = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    repeat(4) {
                        CatalogProviderCardPlaceholder()
                    }
                }
            },
            errorContent = { error ->
                EmptyDataMessage(
                    modifier = Modifier.padding(horizontal = 15.dp),
                    title = stringResource(com.flixclusive.core.presentation.mobile.R.string.an_error_occurred),
                    description = error.message.asString(),
                    icon = {
                        Icon(
                            painter = painterResource(UiCommonR.drawable.round_error_outline_24),
                            contentDescription = stringResource(LocaleR.string.error_icon_content_desc),
                            modifier = Modifier.size(60.dp),
                            tint = MaterialTheme.colorScheme.error.copy(0.6f),
                        )
                    },
                )
            }
        ) { data ->
            CatalogProvidersList(
                providers = data(),
                onSave = { list ->
                    list.forEach {
                        scope.launch { onToggle(it) }
                    }

                    onDismiss()
                }
            )
        }
    }
}

@Composable
private fun CatalogProvidersList(
    providers: List<CatalogProvider>,
    onSave: (List<CatalogProvider>) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val currentProviders = remember {
        mutableStateMapOf<String, CatalogProvider>().also {
            providers.fastForEach { provider ->
                it[provider.id] = provider
            }
        }
    }

    val sortedProviders by remember {
        derivedStateOf { currentProviders.values.sortedBy { it.createdAt } }
    }

    val density = LocalDensity.current
    var buttonHeight by remember { mutableStateOf(40.dp) }
    val isButtonEnabled by remember {
        derivedStateOf {
            currentProviders.values.forEachIndexed { index, wrapper ->
                if (wrapper.isCatalogEnabled != providers[index].isCatalogEnabled) {
                    return@derivedStateOf true
                }
            }

            false
        }
    }

    Box(modifier) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(
                top = buttonHeight,
                bottom = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fadingEdge(
                scrollableState = listState,
                orientation = Orientation.Vertical,
                startEdge = buttonHeight * 2f,
                endEdge = 0.dp
            )
        ) {
            items(
                sortedProviders,
                key = { it.id }
            ) { provider ->
                CatalogProviderCard(
                    provider = provider,
                    enabled = { provider.isCatalogEnabled },
                    onToggle = {
                        val updatedProvider = provider.copy(isCatalogEnabled = !provider.isCatalogEnabled)
                        currentProviders[provider.id] = updatedProvider
                    },
                )
            }
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .onGloballyPositioned { coordinates ->
                    buttonHeight = with(density) { coordinates.size.height.toDp() }
                }
        ) {
            Button(
                enabled = isButtonEnabled,
                shape = MaterialTheme.shapes.small,
                onClick = { onSave(currentProviders.values.toList()) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) {
                Text(text = stringResource(LocaleR.string.save))
            }
        }
    }
}

@Composable
private fun CatalogProviderCard(
    provider: CatalogProvider,
    enabled: () -> Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
        ),
        onClick = onToggle
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp),
        ) {
            ImageWithSmallPlaceholder(
                model = remember { context.buildImageRequest(provider.iconUrl) },
                placeholder = painterResource(UiCommonR.drawable.provider_logo),
                contentDescription = provider.name,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.size(40.dp),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = provider.name,
                    style = MaterialTheme.typography.labelMedium
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "v${provider.versionName} (${provider.versionCode})",
                        style = MaterialTheme.typography.labelSmall,
                        color = LocalContentColor.current.copy(0.6F)
                    )

                    if (provider.status != ProviderStatus.Working) {
                        Text(
                            text = provider.status.name,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            color = provider.status.asStatusColor(),
                            modifier = Modifier
                                .graphicsLayer { alpha = 0.6F }
                        )
                    }
                }
            }

            Switch(
                checked = enabled(),
                enabled = provider.status.isWorking,
                colors = SwitchDefaults.colors(
                    disabledCheckedThumbColor =
                        MaterialTheme.colorScheme.surface
                            .copy(1F)
                            .compositeOver(MaterialTheme.colorScheme.surface),
                    disabledCheckedTrackColor =
                        MaterialTheme.colorScheme.onSurface
                            .copy(0.12F)
                            .compositeOver(MaterialTheme.colorScheme.surface),
                ),
                onCheckedChange = { onToggle() },
                modifier = Modifier
                    .scale(0.7F)
                    .width(40.dp),
            )
        }
    }
}

@Composable
private fun CatalogProviderCardPlaceholder() {
    Placeholder(
        elevation = Elevations.LEVEL_3,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .height(40.dp + 12.dp)
    )
}

@Preview
@Composable
private fun CatalogProvidersBottomSheetPreview() {
    FlixclusiveTheme {
        Surface {
            CatalogProvidersBottomSheet(
                providers = Async.Success(
                    List(20) {
                        CatalogProvider(
                            isCatalogEnabled = true,
                            createdAt = System.currentTimeMillis() - it,
                            provider = DummyDataForPreview.getProviderMetadata(
                                id = "provider_$it",
                                name = "Provider ${it + 1}",
                            )
                        )
                    }
                ),
                onDismiss = {},
                onToggle = {}
            )
        }
    }
}

@Preview
@Composable
private fun CatalogProviderCardPreview() {
    FlixclusiveTheme {
        Surface {
            LazyColumn {
                items(3) {
                    val item = remember {
                        DummyDataForPreview.getProviderMetadata(
                            id = it.toString(),
                            name = "Provider $it",
                            versionName = "1.0.$it",
                            versionCode = 100L + it,
                            status = ProviderStatus.entries[it % ProviderStatus.entries.size]
                        )
                    }

                    CatalogProviderCard(
                        provider = CatalogProvider(
                            isCatalogEnabled = true,
                            provider = item
                        ),
                        enabled = { true },
                        onToggle = {},
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}
