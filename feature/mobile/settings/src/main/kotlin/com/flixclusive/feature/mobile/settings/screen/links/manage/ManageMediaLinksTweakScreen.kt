package com.flixclusive.feature.mobile.settings.screen.links.manage

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.common.domain.Async.Companion.AsyncAnimatedContent
import com.flixclusive.core.database.entity.provider.CachedMediaLink
import com.flixclusive.core.database.entity.provider.CachedStream
import com.flixclusive.core.navigation.navigator.NavigateBack
import com.flixclusive.core.navigation.navigator.NavigateToMediaLinksBottomSheet
import com.flixclusive.core.presentation.common.util.DummyDataForPreview
import com.flixclusive.core.presentation.mobile.components.EmptyDataMessage
import com.flixclusive.core.presentation.mobile.components.Placeholder
import com.flixclusive.core.presentation.mobile.components.material3.PlainTooltipBox
import com.flixclusive.core.presentation.mobile.components.material3.topbar.ActionButton
import com.flixclusive.core.presentation.mobile.components.material3.topbar.CommonTopBar
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.theme.MobileColors.surfaceColorAtElevation
import com.flixclusive.core.presentation.mobile.util.LocalGlobalScaffoldPadding
import com.flixclusive.feature.mobile.settings.screen.links.util.CacheLinksFormatUtil
import com.flixclusive.feature.mobile.settings.screen.links.util.LinkUtil.toRelativeTime
import com.flixclusive.feature.mobile.settings.screen.links.util.PreviewData
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.PartialMedia
import com.flixclusive.model.media.common.tv.Episode
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.ExternalModuleGraph
import kotlinx.coroutines.flow.collectLatest
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

data class ManageMediaLinksTweakScreenArgs(
    val media: MediaMetadata,
    val episode: Episode? = null,
)

interface NavigatorManageMediaLinksTweakScreen :
    NavigateBack,
    NavigateToMediaLinksBottomSheet

@Destination<ExternalModuleGraph>(
    navArgs = ManageMediaLinksTweakScreenArgs::class
)
@Composable
internal fun ManageMediaLinksTweakScreen(
    args: ManageMediaLinksTweakScreenArgs,
    navigator: NavigatorManageMediaLinksTweakScreen,
    viewModel: ManageMediaLinksTweakViewModel = hiltViewModel()
) {
    val links by viewModel.links.collectAsStateWithLifecycle()
    val providerFilters by viewModel.providerFilters.collectAsStateWithLifecycle()
    val typeFilter by viewModel.typeFilter.collectAsStateWithLifecycle()
    val selectedLinks by viewModel.selectedLinks.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.event.collectLatest { event ->
            when (event) {
                is ManageMediaLinksTweakEvent.PlayLink -> {
                    if (event.media !is PartialMedia) {
                        navigator.showPlayerSplashScreen(
                            media = event.media,
                            episode = event.episode,
                            initialStreamUrl = event.link.url,
                            initialHeaders = event.link.customHeaders
                        )
                    }
                }
            }
        }
    }

    ManageMediaLinksTweakScreenContent(
        topbarTitle = CacheLinksFormatUtil.getFormattedTitle(
            media = args.media,
            season = args.episode?.season,
            episode = args.episode?.number
        ),
        links = links,
        providerFilters = providerFilters,
        typeFilter = typeFilter,
        selectedLinks = { selectedLinks },
        onNavigateBack = navigator::navigateBack,
        onTypeFilterChange = viewModel::onUpdateTypeFilter,
        onProviderFilterChange = { viewModel.onUpdateProviderFilter(it.provider.id) },
        onToggleSelect = viewModel::onToggleSelect,
        onClearSelection = viewModel::onClearSelection,
        onDeleteLinks = viewModel::onDeleteLinks,
        onResetLinks = viewModel::onResetLinks,
        onPlayLink = viewModel::onPlayLink
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ManageMediaLinksTweakScreenContent(
    topbarTitle: String,
    links: Async<List<CachedMediaLink>>,
    providerFilters: List<ProviderFilterState>,
    typeFilter: LinkType,
    selectedLinks: () -> Set<CachedMediaLink>,
    onNavigateBack: () -> Unit,
    onTypeFilterChange: (LinkType) -> Unit,
    onProviderFilterChange: (ProviderFilterState) -> Unit,
    onToggleSelect: (CachedMediaLink) -> Unit,
    onClearSelection: () -> Unit,
    onDeleteLinks: (List<CachedMediaLink>) -> Unit,
    onResetLinks: (List<CachedMediaLink>) -> Unit,
    onPlayLink: (CachedMediaLink) -> Unit,
) {
    val selectedCount by remember {
        derivedStateOf { selectedLinks().size }
    }
    val isSelecting by remember {
        derivedStateOf { selectedCount > 0 }
    }

    val titleSelectedCount = remember { mutableIntStateOf(0) }
    LaunchedEffect(selectedCount) {
        if (selectedCount > 0) {
            titleSelectedCount.intValue = selectedCount
        }
    }

    val topBarColor by animateColorAsState(
        targetValue = if (isSelecting) {
            MaterialTheme.colorScheme.surfaceColorAtElevation(3)
        } else {
            MaterialTheme.colorScheme.surface
        },
        label = "TopBarColor"
    )

    Scaffold(
        contentWindowInsets = WindowInsets(),
        topBar = {
            CommonTopBar(
                navigationIcon = {
                    ActionButton(onClick = { if (isSelecting) onClearSelection() else onNavigateBack() }) {
                        AnimatedContent(
                            targetState = isSelecting,
                            label = "TopBarIcon",
                            transitionSpec = {
                                fadeIn() togetherWith fadeOut()
                            }
                        ) { selectionMode ->
                            Icon(
                                painter = painterResource(
                                    if (selectionMode) {
                                        UiCommonR.drawable.round_close_24
                                    } else {
                                        UiCommonR.drawable.left_arrow
                                    }
                                ),
                                contentDescription = null
                            )
                        }
                    }
                },
                title = {
                    AnimatedContent(
                        label = "TopBarTitle",
                        targetState = isSelecting,
                        transitionSpec = { fadeIn() togetherWith fadeOut() }
                    ) { selectionMode ->
                        if (selectionMode) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AnimatedContent(
                                    label = "SelectionCount",
                                    targetState = titleSelectedCount.intValue,
                                    transitionSpec = {
                                        if (targetState > initialState) {
                                            (slideInVertically { height -> height } + fadeIn()) togetherWith
                                                slideOutVertically { height -> -height } + fadeOut()
                                        } else {
                                            (slideInVertically { height -> -height } + fadeIn()) togetherWith
                                                slideOutVertically { height -> height } + fadeOut()
                                        }.using(SizeTransform(clip = false))
                                    }
                                ) { count ->
                                    Text(
                                        text = "$count",
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Text(text = " ${stringResource(LocaleR.string.selected)}")
                            }
                        } else {
                            Text(text = topbarTitle)
                        }
                    }
                },
                actions = {
                    AnimatedVisibility(
                        visible = isSelecting,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PlainTooltipBox(description = stringResource(LocaleR.string.reset)) {
                                ActionButton(onClick = { onResetLinks(emptyList()) }) {
                                    Icon(
                                        painter = painterResource(UiCommonR.drawable.round_refresh_24),
                                        contentDescription = null
                                    )
                                }
                            }

                            PlainTooltipBox(description = stringResource(LocaleR.string.delete)) {
                                ActionButton(onClick = { onDeleteLinks(emptyList()) }) {
                                    Icon(
                                        painter = painterResource(UiCommonR.drawable.delete),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                },
                containerColor = topBarColor
            )
        },
        modifier = Modifier.padding(LocalGlobalScaffoldPadding.current)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Filter Bar
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (links is Async.Loading) {
                    items(4) {
                        Placeholder(
                            modifier = Modifier
                                .size(width = 80.dp, height = 32.dp)
                                .clip(CircleShape)
                        )
                    }
                } else {
                    items(LinkType.entries) { type ->
                        val isSelected = typeFilter == type
                        FilterChip(
                            selected = isSelected,
                            onClick = { onTypeFilterChange(type) },
                            label = { Text(type.name) },
                            shape = CircleShape,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.onSurface,
                                selectedLabelColor = MaterialTheme.colorScheme.surface
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected
                            )
                        )
                    }

                    items(providerFilters) { filter ->
                        FilterChip(
                            selected = filter.selected,
                            onClick = { onProviderFilterChange(filter) },
                            label = { Text(filter.provider.name) },
                            shape = CircleShape
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(top = 8.dp))

            // Links List
            AsyncAnimatedContent(
                targetState = links,
                modifier = Modifier.fillMaxSize(),
                loadingContent = {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(5) {
                            LinkCardPlaceholder()
                        }
                    }
                },
                errorContent = {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = it.message.toString())
                    }
                },
            ) { items ->
                if (items().isEmpty()) {
                    EmptyDataMessage(
                        modifier = Modifier.fillMaxSize(),
                        emojiHeader = "🫥",
                        title = stringResource(LocaleR.string.label_no_cached_media)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (!isSelecting) {
                            item(key = "mass_action_buttons") {
                                MassActionButtons(
                                    onResetAll = { onResetLinks(items()) },
                                    onDeleteAll = { onDeleteLinks(items()) },
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }

                        items(items(), key = { it.url }) { link ->
                            LinkCard(
                                link = link,
                                actionsEnabled = { selectedLinks().isEmpty() },
                                isSelected = { link in selectedLinks() },
                                onToggleSelect = { onToggleSelect(link) },
                                onPlay = { onPlayLink(link) },
                                onDelete = { onDeleteLinks(listOf(link)) },
                                onReset = { onResetLinks(listOf(link)) },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MassActionButtons(
    onResetAll: () -> Unit,
    onDeleteAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onResetAll,
            modifier = Modifier.weight(1f),
            shape = MaterialTheme.shapes.medium,
            contentPadding = PaddingValues(12.dp)
        ) {
            Icon(
                painter = painterResource(UiCommonR.drawable.round_refresh_24),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(LocaleR.string.reset),
                fontWeight = FontWeight.Bold
            )
        }

        TextButton(
            onClick = onDeleteAll,
            modifier = Modifier.weight(1f),
            shape = MaterialTheme.shapes.medium,
            contentPadding = PaddingValues(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(
                painter = painterResource(UiCommonR.drawable.delete),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(LocaleR.string.delete),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LinkCard(
    link: CachedMediaLink,
    actionsEnabled: () -> Boolean,
    isSelected: () -> Boolean,
    onToggleSelect: () -> Unit,
    onPlay: () -> Unit,
    onDelete: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val resources = LocalResources.current
    val isUntested = link.updatedAt == link.createdAt

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .combinedClickable(
                onClick = onPlay,
                onLongClick = onToggleSelect
            ),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = MaterialTheme.shapes.medium,
        border = if (isSelected()) {
            androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.primary
            )
        } else {
            null
        }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                val icon =
                    if (link is CachedStream) {
                        UiCommonR.drawable.play_outline_circle
                    } else {
                        UiCommonR.drawable.outline_subtitles_24
                    }
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .padding(top = 2.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = link.label,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                    )

                    link.description?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }

                StatusBadge(
                    isDead = link.isDead,
                    isUntested = isUntested
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Relative Time
                Text(
                    text = link.updatedAt.toRelativeTime(resources),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.weight(1f))

                PlainTooltipBox(description = stringResource(LocaleR.string.reset)) {
                    IconButton(
                        onClick = onReset,
                        enabled = actionsEnabled(),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            painterResource(UiCommonR.drawable.round_refresh_24),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                PlainTooltipBox(description = stringResource(LocaleR.string.play)) {
                    IconButton(
                        onClick = onPlay,
                        enabled = actionsEnabled(),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            painterResource(UiCommonR.drawable.play),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                PlainTooltipBox(description = stringResource(LocaleR.string.delete)) {
                    IconButton(
                        onClick = onDelete,
                        enabled = actionsEnabled(),
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            painterResource(UiCommonR.drawable.delete),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LinkCardPlaceholder() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Placeholder(modifier = Modifier.size(20.dp), shape = CircleShape)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Placeholder(modifier = Modifier.fillMaxWidth(0.6f).height(16.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Placeholder(modifier = Modifier.fillMaxWidth(0.4f).height(12.dp))
                }
                Placeholder(modifier = Modifier.size(8.dp), shape = CircleShape)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Placeholder(modifier = Modifier.size(width = 60.dp, height = 16.dp))
                Spacer(modifier = Modifier.weight(1f))
                repeat(3) {
                    Placeholder(modifier = Modifier.size(32.dp), shape = CircleShape)
                    Spacer(modifier = Modifier.width(4.dp))
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(isDead: Boolean, isUntested: Boolean) {
    if (isUntested) return

    val color = if (isDead) MaterialTheme.colorScheme.error else Color(0xFF4CAF50)

    Box(
        modifier = Modifier
            .size(8.dp)
            .background(color, CircleShape)
    )
}

@Preview
@Composable
private fun LinkCardPreview() {
    FlixclusiveTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LinkCard(
                    actionsEnabled = { false },
                    link = PreviewData.getStream(),
                    isSelected = { false },
                    onToggleSelect = {},
                    onPlay = {},
                    onDelete = {},
                    onReset = {}
                )

                LinkCard(
                    actionsEnabled = { true },
                    link = PreviewData.getStream().copy(isDead = true),
                    isSelected = { true },
                    onToggleSelect = {},
                    onPlay = {},
                    onDelete = {},
                    onReset = {}
                )
            }
        }
    }
}

@Preview
@Composable
private fun ManageMediaLinksTweakScreenPreview() {
    val dummyStreams = List(5) { i ->
        CachedStream(
            url = "https://example.com/video_$i.mkv",
            label = "Example Stream $i",
            description = "1080p • 2.3 GB",
            isDead = i % 2 == 0,
            createdAt = java.util.Date(),
            updatedAt = java.util.Date(),
            providerId = "provider-1",
            ownerId = "owner-1",
            mediaId = "media-1"
        )
    }

    FlixclusiveTheme {
        Surface {
            ManageMediaLinksTweakScreenContent(
                topbarTitle = "",
                links = Async.Success(dummyStreams),
                providerFilters = listOf(
                    ProviderFilterState(DummyDataForPreview.getProviderMetadata(), true),
                    ProviderFilterState(DummyDataForPreview.getProviderMetadata().copy(name = "Provider 2"), false)
                ),
                typeFilter = LinkType.All,
                selectedLinks = { emptySet() },
                onNavigateBack = {},
                onTypeFilterChange = {},
                onProviderFilterChange = {},
                onToggleSelect = {},
                onClearSelection = {},
                onDeleteLinks = {},
                onResetLinks = {},
                onPlayLink = {},
            )
        }
    }
}
