package com.flixclusive.feature.mobile.provider.test

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastSumBy
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.navigation.navigator.GoBackAction
import com.flixclusive.core.presentation.common.extensions.buildImageRequest
import com.flixclusive.core.presentation.common.util.DummyDataForPreview.getDummyProviderMetadata
import com.flixclusive.core.presentation.mobile.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.core.presentation.mobile.extensions.isCompact
import com.flixclusive.core.presentation.mobile.extensions.isExpanded
import com.flixclusive.core.presentation.mobile.extensions.isMedium
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.LocalGlobalScaffoldPadding
import com.flixclusive.domain.provider.testing.TestJobState
import com.flixclusive.domain.provider.testing.TestStage
import com.flixclusive.domain.provider.testing.model.ProviderTestCaseResult
import com.flixclusive.domain.provider.testing.model.ProviderTestResult
import com.flixclusive.domain.provider.testing.model.TestStatus
import com.flixclusive.feature.mobile.provider.test.component.ButtonControllerDivider
import com.flixclusive.feature.mobile.provider.test.component.FullLogDialog
import com.flixclusive.feature.mobile.provider.test.component.ProviderTestScreenTopBar
import com.flixclusive.feature.mobile.provider.test.component.RepetitiveTestNoticeDialog
import com.flixclusive.feature.mobile.provider.test.component.SortBottomSheet
import com.flixclusive.feature.mobile.provider.test.component.TestResultCard
import com.flixclusive.feature.mobile.provider.test.component.TestScreenHeader
import com.flixclusive.model.provider.ProviderMetadata
import com.ramcosta.composedestinations.annotation.Destination
import kotlinx.coroutines.flow.first
import kotlin.random.Random
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import com.flixclusive.core.strings.R as LocaleR

@Suppress("ktlint:compose:mutable-params-check")
@Destination
@Composable
internal fun ProviderTestScreen(
    navigator: GoBackAction,
    providers: ArrayList<ProviderMetadata>,
    viewModel: ProviderTestScreenViewModel = hiltViewModel(),
) {
    val results by viewModel.results.collectAsStateWithLifecycle()
    val testStage by viewModel.testStage.collectAsStateWithLifecycle()
    val testJobState by viewModel.testJobState.collectAsStateWithLifecycle()
    val filmOnTest by viewModel.filmOnTest.collectAsStateWithLifecycle()

    ProviderTestScreenContent(
        results = results,
        stage = testStage,
        testJobState = testJobState,
        filmOnTest = filmOnTest,
        onGoBack = navigator::goBack,
        onPauseTests = viewModel::pauseTests,
        onResumeTests = viewModel::resumeTests,
        onStopTests = viewModel::stopTests,
        onClearTests = viewModel::clearTests,
        onTestAllProviders = { viewModel.startTests(providers = providers) },
        onRetestAllProviders = {
            viewModel.startTests(
                providers = providers,
                testAgainIfTested = true,
            )
        },
        onSkipTestedProviders = {
            viewModel.startTests(
                providers = providers,
                skipTestedProviders = true,
            )
        },
    )
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ProviderTestScreenContent(
    results: List<ProviderTestResult>,
    stage: TestStage,
    testJobState: TestJobState,
    filmOnTest: String?,
    onGoBack: () -> Unit,
    onTestAllProviders: () -> StartTestResult,
    onRetestAllProviders: () -> Unit,
    onSkipTestedProviders: () -> Unit,
    onPauseTests: () -> Unit,
    onResumeTests: () -> Unit,
    onStopTests: () -> Unit,
    onClearTests: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // Used to track the expanded state of each test result card
    // This is needed because the list is sorted and recomposed often
    val testResultCardsIsExpandedMap = remember { mutableStateMapOf<String, Boolean>() }

    var showRepetitiveTestWarning by rememberSaveable { mutableStateOf(false) }
    var testCaseResultToShow by remember { mutableStateOf<Pair<ProviderMetadata, ProviderTestCaseResult>?>(null) }

    var isSortingBottomSheetOpen by rememberSaveable { mutableStateOf(false) }
    var sortOption by remember { mutableStateOf(SortOption(sort = SortOption.SortType.Date)) }

    val context = LocalContext.current
    val localDensity = LocalDensity.current

    val windowWidthSizeClass = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass

    val gridColumns = when {
        windowWidthSizeClass.isCompact -> 1
        windowWidthSizeClass.isMedium -> 2
        windowWidthSizeClass.isExpanded -> 3
        else -> 1
    }

    var headerHeight by remember { mutableStateOf(0.dp) }
    var headerHeightPx by remember { mutableFloatStateOf(0F) }

    val listState = rememberLazyGridState()
    val topBarBackgroundAlpha by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex == 0) {
                (listState.firstVisibleItemScrollOffset.toFloat() / headerHeightPx).coerceIn(0F, 1F)
            } else {
                1F
            }
        }
    }

    val topBarBackgroundColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.surface.copy(alpha = topBarBackgroundAlpha),
        label = "",
    )

    LaunchedEffect(stage, results.size) {
        val resultsAreNotEmpty = results.isNotEmpty()
        if (stage.isIdle && resultsAreNotEmpty) {
            val totalTestsPerformed = results.fastSumBy { it.outputs.first().size }
            val totalTestsPassed = results.fastSumBy { result ->
                result.outputs.first().count { it.isSuccess }
            }

            snackbarHostState.showSnackbar(
                message = context.getString(
                    LocaleR.string.test_providers_completed,
                    "$totalTestsPassed/$totalTestsPerformed",
                ),
                duration = SnackbarDuration.Indefinite,
                withDismissAction = true,
            )
        } else if (!stage.isIdle) {
            snackbarHostState.currentSnackbarData?.dismiss()
        }
    }

    val testResults by remember {
        derivedStateOf {
            results.sortedWith { a, b ->
                compareTestResults(
                    a = a,
                    b = b,
                    sortOption = sortOption,
                )
            }
        }
    }

    Scaffold(
        modifier = Modifier.padding(LocalGlobalScaffoldPadding.current),
        contentWindowInsets = WindowInsets(0.dp),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            ProviderTestScreenTopBar(
                onNavigationIconClick = onGoBack,
                onOpenSortBottomSheet = { isSortingBottomSheetOpen = true },
                modifier =
                    Modifier
                        .background(topBarBackgroundColor),
            )
        },
    ) {
        LazyVerticalGrid(
            modifier = Modifier.padding(horizontal = 10.dp),
            columns = GridCells.Fixed(gridColumns),
            state = listState,
            contentPadding = it,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box {
                    AnimatedVisibility(
                        visible = headerHeight > 0.dp && filmOnTest != null,
                        enter = fadeIn(initialAlpha = 0.3F),
                        exit = fadeOut(targetAlpha = 0.3F),
                    ) {
                        AsyncImage(
                            model = context.buildImageRequest(
                                imagePath = filmOnTest,
                                imageSize = "w600_and_h900_multi_faces",
                            ),
                            contentScale = ContentScale.Crop,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(headerHeight)
                                .drawScrimOnForeground(MaterialTheme.colorScheme.surface),
                        )
                    }

                    Column {
                        Column(
                            modifier =
                                Modifier.onGloballyPositioned { coordinates ->
                                    headerHeightPx = coordinates.size.height.toFloat()
                                    headerHeight = with(localDensity) { coordinates.size.height.toDp() }
                                },
                        ) {
                            TestScreenHeader(stage = stage)

                            ButtonControllerDivider(
                                testJobState = testJobState,
                                onStop = onStopTests,
                                onPause = onPauseTests,
                                onResume = onResumeTests,
                                onStart = {
                                    when (onTestAllProviders()) {
                                        StartTestResult.SHOW_WARNING -> showRepetitiveTestWarning = true
                                        StartTestResult.STARTED -> {
                                            // No op
                                        }
                                    }
                                },
                            )
                        }

                        OutlinedButton(
                            onClick = onClearTests,
                            enabled = stage.isIdle && results.isNotEmpty(),
                            shape = MaterialTheme.shapes.extraSmall,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                        ) {
                            Text(
                                text = stringResource(id = LocaleR.string.clear_tests),
                                color = MaterialTheme.colorScheme.onSurface.copy(0.6f),
                                style = LocalTextStyle.current.asAdaptiveTextStyle(),
                            )
                        }
                    }
                }
            }

            items(
                items = testResults,
                key = { it.provider.id },
            ) { data ->
                TestResultCard(
                    isExpanded = testResultCardsIsExpandedMap.getOrElse(data.provider.id) { false },
                    testResult = data,
                    onToggle = {
                        val state = testResultCardsIsExpandedMap.getOrElse(data.provider.id) { false }
                        testResultCardsIsExpandedMap[data.provider.id] = !state
                    },
                    showFullLog = { testCaseResultToShow = data.provider to it },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }

    if (showRepetitiveTestWarning) {
        RepetitiveTestNoticeDialog(
            onSkip = onSkipTestedProviders,
            onTestAgain = onRetestAllProviders,
            onDismiss = { showRepetitiveTestWarning = false },
        )
    }

    if (isSortingBottomSheetOpen) {
        SortBottomSheet(
            selectedSortOption = sortOption,
            onSort = { sortOption = it },
            onDismiss = { isSortingBottomSheetOpen = false },
        )
    }

    if (testCaseResultToShow != null) {
        val (provider, testCaseOutput) = testCaseResultToShow!!

        FullLogDialog(
            testCaseOutput = testCaseOutput,
            provider = provider,
            onDismiss = { testCaseResultToShow = null },
        )
    }
}

private fun compareTestResults(
    a: ProviderTestResult,
    b: ProviderTestResult,
    sortOption: SortOption,
): Int {
    val comparison = when (sortOption.sort) {
        SortOption.SortType.Name -> a.provider.name.compareTo(b.provider.name)
        SortOption.SortType.Date -> a.date.time.compareTo(b.date.time)
        SortOption.SortType.Score -> {
            val passedCountA = a.outputs.value.count { it.isSuccess }
            val passedCountB = b.outputs.value.count { it.isSuccess }
            passedCountA.compareTo(passedCountB)
        }
    }

    return if (sortOption.ascending) comparison else -comparison
}

private fun Modifier.drawScrimOnForeground(scrimColor: Color) =
    drawWithCache {
        onDrawWithContent {
            drawContent()
            drawRect(
                brush =
                    Brush.verticalGradient(
                        0F to scrimColor.copy(alpha = 0.8F),
                        0.8F to scrimColor,
                    ),
            )
        }
    }

@Preview
@Composable
private fun ProviderTestScreenBasePreview() {
    val results = remember {
        List(5) {
            ProviderTestResult(
                provider = getDummyProviderMetadata().copy(id = it.toString()),
                date = java.util.Date(),
            ).also { result ->
                repeat(5) {
                    result.add(
                        ProviderTestCaseResult(
                            status = TestStatus.entries[Random.nextInt(3)],
                            name = UiText.from("Test case #$it"),
                            timeTaken = Random
                                .nextLong(1000, 5000)
                                .toDuration(DurationUnit.SECONDS),
                        ),
                    )
                }
            }
        }
    }

    FlixclusiveTheme {
        Surface {
            ProviderTestScreenContent(
                results = results,
                stage = TestStage.Idle,
                testJobState = TestJobState.IDLE,
                filmOnTest = "https://image.tmdb.org/t/p/w600_and_h900_multi_faces/8UlWHLMpgZm9bx6QYh0NFoq67TZ.jpg",
                onGoBack = {},
                onPauseTests = {},
                onResumeTests = {},
                onStopTests = {},
                onClearTests = {},
                onTestAllProviders = { StartTestResult.STARTED },
                onRetestAllProviders = {},
                onSkipTestedProviders = {},
            )
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun ProviderTestScreenCompactLandscapePreview() {
    ProviderTestScreenBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun ProviderTestScreenMediumPortraitPreview() {
    ProviderTestScreenBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun ProviderTestScreenMediumLandscapePreview() {
    ProviderTestScreenBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun ProviderTestScreenExtendedPortraitPreview() {
    ProviderTestScreenBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun ProviderTestScreenExtendedLandscapePreview() {
    ProviderTestScreenBasePreview()
}
