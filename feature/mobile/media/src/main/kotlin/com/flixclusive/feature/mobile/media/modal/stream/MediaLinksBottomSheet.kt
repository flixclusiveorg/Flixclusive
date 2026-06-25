package com.flixclusive.feature.mobile.media.modal.stream

import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastAny
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.common.provider.LoadLinksState
import com.flixclusive.core.database.entity.provider.CachedMediaLink
import com.flixclusive.core.database.entity.provider.CachedStream
import com.flixclusive.core.database.entity.provider.CachedSubtitle
import com.flixclusive.core.navigation.navigator.NavigateToMediaLinksBottomSheet
import com.flixclusive.core.presentation.common.components.GradientLinearProgressIndicator
import com.flixclusive.core.presentation.common.extensions.getActivity
import com.flixclusive.core.presentation.common.util.CustomClipboardManager.Companion.rememberClipboardManager
import com.flixclusive.core.presentation.mobile.components.EmptyDataMessage
import com.flixclusive.core.presentation.mobile.components.ImageWithSmallPlaceholder
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.theme.MobileColors.surfaceColorAtElevation
import com.flixclusive.feature.mobile.media.R
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.common.tv.Episode
import com.flixclusive.model.provider.link.Flag
import com.flixclusive.model.provider.link.MediaLink
import com.flixclusive.model.provider.link.Stream
import com.flixclusive.model.provider.link.Subtitle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.ExternalModuleGraph
import com.ramcosta.composedestinations.bottomsheet.spec.DestinationStyleBottomSheet
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.presentation.mobile.R as UiMobileR
import com.flixclusive.core.strings.R as LocaleR

private val MediaLinkCardMinHeight = 60.dp
private val MediaLinkImageCardSize = 50.dp
private val MediaLinkCardIconSize = 20.dp

data class MediaLinksBottomSheetArgs(
    val media: MediaMetadata,
    val episode: Episode? = null,
)

private val List<CachedMediaLink>.hasPlayableLinks: Boolean
    get() {
        return fastAny { it is CachedStream && !it.isThirdPartyGateway }
    }

private val List<CachedMediaLink>.hasValidLinks: Boolean
    get() {
        return fastAny { it.isValid }
    }

@OptIn(FlowPreview::class)
@Destination<ExternalModuleGraph>(
    style = DestinationStyleBottomSheet::class,
    navArgs = MediaLinksBottomSheetArgs::class
)
@Composable
internal fun MediaLinksBottomSheet(
    navigator: NavigateToMediaLinksBottomSheet,
    viewModel: MediaLinksBottomSheetViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playerPrefs by viewModel.playerPrefs.collectAsStateWithLifecycle()
    val links by viewModel.links.collectAsStateWithLifecycle()

    val activity = LocalContext.current.getActivity<ComponentActivity>()
    val window = activity.window

    DisposableEffect(true) {
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    LaunchedEffect(viewModel, playerPrefs) {
        combine(
            viewModel.uiState.map { it.loadLinksState }.distinctUntilChanged(),
            viewModel.links
        ) { state, links ->
            playerPrefs.isAutoSelectingServer &&
                !state.isLoading &&
                links.hasPlayableLinks &&
                state.isSuccess
        }.filter { it }
            .distinctUntilChanged()
            .debounce(1000L.milliseconds) // Debounce to prevent rapid navigation if links change quickly
            .collectLatest {
                navigator.showPlayerSplashScreen(
                    media = uiState.metadata,
                    episode = uiState.episode,
                )
            }
    }

    MediaLinksBottomSheetContent(
        state = { uiState.loadLinksState },
        links = { links },
        canSkipLoading = {
            playerPrefs.isAutoSelectingServer &&
                links.fastAny { it.isValid } &&
                uiState.loadLinksState.isLoading
        },
        canAutoSelectStream = { playerPrefs.isAutoSelectingServer },
        onResetAndRetry = viewModel::onResetAndRetry,
        onSkipLoading = {
            navigator.showPlayerSplashScreen(
                media = uiState.metadata,
                episode = uiState.episode,
            )
        },
        onPlayLink = {
            navigator.showPlayerSplashScreen(
                media = uiState.metadata,
                episode = uiState.episode,
                initialStreamUrl = it.url,
                initialHeaders = it.customHeaders
            )
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MediaLinksBottomSheetContent(
    state: () -> LoadLinksState,
    links: () -> List<CachedMediaLink>,
    canSkipLoading: () -> Boolean,
    canAutoSelectStream: () -> Boolean,
    onPlayLink: (CachedStream) -> Unit,
    onResetAndRetry: () -> Unit,
    onSkipLoading: () -> Unit,
) {
    val combinedLinks by remember {
        derivedStateOf {
            links().sortedByDescending { it is CachedStream }
        }
    }

    val hasValidLinks by remember {
        derivedStateOf { links().hasValidLinks }
    }

    val isLoading by remember {
        derivedStateOf {
            state().isLoading ||
                (
                    state().isSuccess &&
                        links().hasValidLinks &&
                        links().hasPlayableLinks &&
                        canAutoSelectStream()
                )
        }
    }

    val showActions by remember {
        derivedStateOf {
            val currentState = state()
            val currentLinks = links()
            currentState.isSuccess || (currentState.isError && currentLinks.hasValidLinks)
        }
    }

    Surface(
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(level = 1),
        shape = MaterialTheme.shapes.small.copy(
            bottomEnd = CornerSize(0.dp),
            bottomStart = CornerSize(0.dp),
        ),
        modifier = Modifier
            .fillMaxWidth()
    ) {
        LazyColumn(
            contentPadding = PaddingValues(10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.padding(vertical = 20.dp)
        ) {
            if (isLoading) {
                item(
                    key = "progress_header"
                ) {
                    ProgressHeader(
                        state = state,
                        canSkipLoading = canSkipLoading,
                        onSkipLoading = onSkipLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem(),
                    )
                }
            }

            if (showActions) {
                item(
                    key = "actions_row"
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .animateItem(),
                    ) {
                        Button(
                            onClick = onResetAndRetry,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(text = stringResource(LocaleR.string.reset_and_retry))
                        }
                    }
                }
            }

            item {
                val hasErrors by remember {
                    derivedStateOf {
                        val currentLinks = links()
                        state().isError && !currentLinks.hasValidLinks
                    }
                }

                AnimatedVisibility(
                    visible = hasErrors,
                    enter = scaleIn() + fadeIn(),
                    exit = fadeOut() + scaleOut(),
                ) {
                    ErrorMessage(
                        state = state(),
                        modifier = Modifier
                            .padding(vertical = 20.dp)
                            .fillMaxSize(),
                    )
                }
            }

            if (hasValidLinks) {
                item {
                    Spacer(modifier = Modifier.padding(top = 10.dp))
                }

                if (state().isError) {
                    item {
                        ErrorItem(
                            error = state().message,
                            modifier = Modifier.animateItem(),
                        )
                    }
                }

                items(
                    combinedLinks,
                    key = { it.hashCode() }
                ) {
                    MediaLinkItem(
                        link = it,
                        modifier = Modifier.animateItem(),
                        onClick = {
                            if (it is CachedStream) {
                                onPlayLink(it)
                            }
                        },
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars))
            }
        }
    }
}

@Composable
private fun ProgressHeader(
    state: () -> LoadLinksState,
    canSkipLoading: () -> Boolean,
    onSkipLoading: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        AnimatedContent(
            targetState = state(),
            contentAlignment = Alignment.Center,
            transitionSpec = {
                val fadeSpec = tween<Float>(durationMillis = 320, easing = FastOutSlowInEasing)
                val slideSpec = tween<IntOffset>(durationMillis = 320, easing = FastOutSlowInEasing)

                if (targetState > initialState) {
                    fadeIn(fadeSpec) + slideInHorizontally(slideSpec) { it / 10 } togetherWith
                        fadeOut(fadeSpec) + slideOutHorizontally(slideSpec) { -it / 10 }
                } else {
                    fadeIn(fadeSpec) + slideInHorizontally(slideSpec) { -it / 10 } togetherWith
                        fadeOut(fadeSpec) + slideOutHorizontally(slideSpec) { it / 10 }
                }.using(SizeTransform(clip = false))
            },
        ) {
            Text(
                text = it.message.asString().trim(),
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp),
            )
        }

        GradientLinearProgressIndicator(
            modifier = Modifier.fillMaxWidth(0.7F),
            colors = listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.tertiary,
            ),
        )

        if (canSkipLoading()) {
            ElevatedButton(
                onClick = onSkipLoading,
                shape = MaterialTheme.shapes.extraSmall,
                contentPadding = PaddingValues(horizontal = 16.dp),
                modifier =
                    Modifier
                        .height(30.dp),
            ) {
                Text(
                    text = stringResource(id = LocaleR.string.label_skip_loading),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun ErrorMessage(
    state: LoadLinksState,
    modifier: Modifier = Modifier,
) {
    val resources = LocalResources.current

    fun getTitle(): String {
        return if (state.isIdle || state.isUnavailable) {
            resources.getString(R.string.label_empty_streams)
        } else {
            resources.getString(LocaleR.string.something_went_wrong)
        }
    }

    fun getDescription(): String {
        return if (state.isIdle) {
            resources.getString(LocaleR.string.empty_data_default_sub_label)
        } else {
            state.message.asString(resources)
        }
    }

    var title by remember { mutableStateOf(getTitle()) }
    var description by remember { mutableStateOf(getDescription()) }

    LaunchedEffect(state) {
        // Small delay to ensure the error message doesn't flash too quickly for fast operations
        delay(800L.milliseconds)

        title = getTitle()
        description = getDescription()
    }

    EmptyDataMessage(
        modifier = modifier,
        title = title,
        description = description,
        emojiHeader = "🫗",
        icon = if (state.isUnavailable || state.isIdle) {
            null
        } else {
            {
                Icon(
                    painter = painterResource(id = UiCommonR.drawable.round_error_outline_24),
                    tint = MaterialTheme.colorScheme.error.copy(0.6f),
                    contentDescription = stringResource(id = LocaleR.string.error_icon_content_desc),
                    modifier = Modifier.size(60.dp),
                )
            }
        },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaLinkItem(
    link: CachedMediaLink,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val clipboardManager = rememberClipboardManager()
    val thirdPartyFlag = remember {
        if (link is CachedStream && link.isThirdPartyGateway) {
            Flag.ThirdPartyGateway(
                name = link.thirdPartyGatewayName ?: return@remember null,
                logo = link.thirdPartyGatewayLogo ?: return@remember null,
            )
        } else {
            null
        }
    }
    val requiresUriHandling = thirdPartyFlag != null

    val clickLink = {
        when {
            requiresUriHandling -> uriHandler.openUri(uri = link.url)
            link is CachedSubtitle -> Unit
            else -> onClick.invoke()
        }
    }

    Card(
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(5.dp),
        ),
        modifier = modifier
            .combinedClickable(
                onClick = clickLink,
                onLongClick = {
                    clipboardManager.setText(
                        if (link.customHeaders?.isNotEmpty() == true) {
                            buildString {
                                append("Name: ${link.label}\n")
                                append("URL: ${link.url}\n")
                                append("Headers:\n")
                                link.customHeaders?.forEach { (key, value) ->
                                    append("  - $key: $value\n")
                                }
                            }
                        } else {
                            link.url
                        }
                    )
                },
            ),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .heightIn(min = MediaLinkCardMinHeight)
                .padding(10.dp),
        ) {
            if (requiresUriHandling) {
                ImageWithSmallPlaceholder(
                    urlImage = thirdPartyFlag.logo,
                    placeholder = painterResource(UiCommonR.drawable.provider_logo),
                    contentDescription = thirdPartyFlag.name,
                    shape = MaterialTheme.shapes.extraSmall,
                    modifier = Modifier
                        .size(MediaLinkImageCardSize),
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.weight(1F)
            ) {
                Text(
                    text = link.label.trim(),
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelLarge,
                )

                Text(
                    text = link.description ?: link.url,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = if (link.description == null) 1 else Int.MAX_VALUE,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = LocalContentColor.current.copy(0.6f),
                    ),
                )

                if (!requiresUriHandling) {
                    MediaLinkIndicatorChip(
                        link = link,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }

            if (link is CachedStream && requiresUriHandling) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .padding(
                            top = (MediaLinkImageCardSize / 2) - (MediaLinkCardIconSize / 2),
                        )
                ) {
                    Icon(
                        painter = painterResource(id = UiCommonR.drawable.web_browser),
                        tint = LocalContentColor.current.copy(0.6f),
                        contentDescription = stringResource(id = LocaleR.string.open_in_web),
                        modifier = Modifier
                            .size(MediaLinkCardIconSize),
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaLinkIndicatorChip(
    link: CachedMediaLink,
    modifier: Modifier = Modifier
) {
    val indicatorColor = when (link) {
        is CachedStream -> MaterialTheme.colorScheme.tertiary
        is CachedSubtitle -> MaterialTheme.colorScheme.onSurface
    }

    Text(
        text = when (link) {
            is CachedSubtitle -> stringResource(id = LocaleR.string.subtitle)
            else -> stringResource(id = LocaleR.string.stream)
        },
        style = MaterialTheme.typography.labelSmall,
        color = indicatorColor,
        fontSize = 10.sp,
        modifier = modifier
            .padding(bottom = 3.dp)
            .background(
                color = indicatorColor.copy(0.1f),
                shape = MaterialTheme.shapes.extraSmall,
            ).padding(horizontal = 5.dp)
    )
}

@Composable
private fun ErrorItem(
    error: UiText?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val defaultDescription = stringResource(LocaleR.string.unknown_season)
    val message = remember { error?.asString(context) ?: defaultDescription }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(3.dp)
            .background(
                color = MaterialTheme.colorScheme.error.copy(0.1f),
                shape = MaterialTheme.shapes.small
            ).border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.error.copy(0.6f),
                shape = MaterialTheme.shapes.small
            )
    ) {
        EmptyDataMessage(
            title = stringResource(UiMobileR.string.an_error_occurred),
            description = message,
            icon = {},
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .align(Alignment.Center)
                .padding(16.dp),
        )
    }
}

@Preview
@Composable
private fun MediaLinksBottomSheetContentPreview() {
    val links = remember { mutableStateListOf<MediaLink>() }
    var state by remember { mutableStateOf<LoadLinksState>(LoadLinksState.Idle) }

    LaunchedEffect(true) {
        var itemCount = 0
        val delayTime = 400L.milliseconds

        delay(delayTime)
        state = LoadLinksState.Fetching()
        delay(delayTime)
        state = LoadLinksState.Extracting("The movie db is being scraped for links.")
        while (itemCount < 10) {
            val randomBool = Random.nextBoolean()
            val link =
                if (itemCount % 2 == 0) {
                    Stream(
                        name = "Server $itemCount",
                        description = if (randomBool) "Lorem ipsum dolor sit amet, consectetur;" else null,
                        url = "https://www.google.com/search?q=flixclusive&oq=flixclusive",
                        flags =
                            if (randomBool) {
                                setOf(
                                    Flag.ThirdPartyGateway(
                                        name = "Netflix",
                                        logo = """
                                            https://media.themoviedb.org/t/p/original/9BgaNQRMDvVlji1JBZi6tcfxpKx.jpg
                                        """.trimIndent(),
                                    ),
                                )
                            } else {
                                null
                            },
                    )
                } else {
                    Subtitle(
                        language = "Sub $itemCount",
                        url = "https://www.google.com",
                    )
                }

            delay(delayTime)
            links.add(link)
            itemCount++
        }
        delay(delayTime)
        state = LoadLinksState.Success
        delay(delayTime)
        state = LoadLinksState.Unavailable()
        delay(delayTime * 3)
        state = LoadLinksState.Error()
    }

    FlixclusiveTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
        ) {
            MediaLinksBottomSheetContent(
                state = { state },
                links = { emptyList() },
                canSkipLoading = { true },
                canAutoSelectStream = { true },
                onPlayLink = {},
                onResetAndRetry = {},
                onSkipLoading = {}
            )
        }
    }
}
