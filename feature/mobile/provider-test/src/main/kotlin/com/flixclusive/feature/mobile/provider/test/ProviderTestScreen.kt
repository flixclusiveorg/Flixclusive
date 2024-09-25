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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import coil.compose.AsyncImage
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.navigation.GoBackAction
import com.flixclusive.core.ui.common.navigation.navargs.ProviderTestScreenNavArgs
import com.flixclusive.core.ui.common.util.DummyDataForPreview.getDummyProviderData
import com.flixclusive.core.ui.common.util.buildImageUrl
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.domain.provider.test.ProviderTestCaseOutput
import com.flixclusive.domain.provider.test.ProviderTestResult
import com.flixclusive.domain.provider.test.TestStage.Idle.Companion.isIdle
import com.flixclusive.feature.mobile.provider.test.component.ButtonControllerDivider
import com.flixclusive.feature.mobile.provider.test.component.FullLogDialog
import com.flixclusive.feature.mobile.provider.test.component.ProviderTestScreenTopBar
import com.flixclusive.feature.mobile.provider.test.component.RepetitiveTestNoticeDialog
import com.flixclusive.feature.mobile.provider.test.component.SortBottomSheet
import com.flixclusive.feature.mobile.provider.test.component.TestResultCard
import com.flixclusive.feature.mobile.provider.test.component.TestScreenHeader
import com.flixclusive.model.provider.ProviderData
import com.ramcosta.composedestinations.annotation.Destination
import com.flixclusive.core.locale.R as LocaleR

@Composable
private fun Modifier.drawScrimOnForeground(
    scrimColor: Color = MaterialTheme.colorScheme.surface
) = drawWithCache {
        onDrawWithContent {
            drawContent()
            drawRect(
                brush = Brush.verticalGradient(
                    0F to scrimColor.copy(alpha = 0.8F),
                    0.8F to scrimColor
                )
            )
        }
    }

@OptIn(ExperimentalFoundationApi::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Destination(
    navArgsDelegate = ProviderTestScreenNavArgs::class
)
@Composable
internal fun ProviderTestScreen(
    navigator: GoBackAction,
    args: ProviderTestScreenNavArgs
) {
     val viewModel = hiltViewModel<ProviderTestScreenViewModel>()

    val stage by viewModel.testProviderUseCase.testStage.collectAsStateWithLifecycle()
    val testJobState by viewModel.testProviderUseCase.testJobState.collectAsStateWithLifecycle()
    val filmOnTest by viewModel.testProviderUseCase.filmOnTest.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    var isSortingBottomSheetOpen by rememberSaveable { mutableStateOf(false) }
    var testCaseOutputToShow by remember { mutableStateOf<Pair<ProviderData, ProviderTestCaseOutput>?>(null) }

    val context = LocalContext.current
    val localDensity = LocalDensity.current
    var headerHeight by remember { mutableStateOf(0.dp) }
    var headerHeightPx by remember { mutableFloatStateOf(0F) }

    val listState = rememberLazyListState()
    val topBarBackgroundAlpha by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex == 0) {
                (listState.firstVisibleItemScrollOffset.toFloat() / headerHeightPx).coerceIn(0F, 1F)
            } else 1F
        }
    }

    val topBarBackgroundColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.surface.copy(alpha = topBarBackgroundAlpha),
        label = ""
    )

    LaunchedEffect(stage, viewModel.testProviderUseCase.results.size) {
        val resultsAreNotEmpty = viewModel.testProviderUseCase.results.isNotEmpty()
        if (stage.isIdle && resultsAreNotEmpty) {
            val totalTestsPerformed
                = viewModel.testProviderUseCase.results
                    .fastSumBy { it.outputs.size }
            val totalTestsPassed
                = viewModel.testProviderUseCase.results
                    .fastSumBy { result ->
                        result.outputs.count { it.isSuccess }
                    }

            snackbarHostState.showSnackbar(
                message = context.getString(LocaleR.string.test_providers_completed, "$totalTestsPassed/$totalTestsPerformed"),
                duration = SnackbarDuration.Indefinite,
                withDismissAction = true
            )
        } else if (!stage.isIdle) {
            snackbarHostState.currentSnackbarData?.dismiss()
        }
    }

    val testResults by remember {
        derivedStateOf {
            viewModel.testProviderUseCase.results.sortedWith { a, b ->
                compareTestResults(
                    a = a,
                    b = b,
                    sortOption = viewModel.sortOption
                )
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            ProviderTestScreenTopBar(
                onNavigationIconClick = navigator::goBack,
                onOpenSortBottomSheet = { isSortingBottomSheetOpen = true },
                modifier = Modifier
                    .background(topBarBackgroundColor)
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier.padding(horizontal = 10.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Box {
                    AnimatedVisibility(
                        visible = headerHeight > 0.dp && filmOnTest != null,
                        enter = fadeIn(initialAlpha = 0.3F),
                        exit = fadeOut(targetAlpha = 0.3F)
                    ) {
                        AsyncImage(
                            model = context.buildImageUrl(
                                imagePath = filmOnTest,
                                imageSize = "w600_and_h900_multi_faces"
                            ),
                            contentScale = ContentScale.Crop,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(headerHeight)
                                .drawScrimOnForeground()
                        )
                    }

                    Column {
                        Column(
                            modifier = Modifier.onGloballyPositioned { coordinates ->
                                headerHeightPx = coordinates.size.height.toFloat()
                                headerHeight = with(localDensity) { coordinates.size.height.toDp() }
                            }
                        ) {
                            TestScreenHeader(stage = stage)

                            ButtonControllerDivider(
                                testJobState = testJobState,
                                onStop = viewModel::stopTests,
                                onPause = viewModel::pauseTests,
                                onResume = viewModel::resumeTests,
                                onStart = { viewModel.startTests(args.providers) }
                            )
                        }

                        OutlinedButton(
                            onClick = viewModel::clearTests,
                            enabled = stage.isIdle && viewModel.testProviderUseCase.results.isNotEmpty(),
                            shape = MaterialTheme.shapes.extraSmall,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                        ) {
                            Text(
                                text = stringResource(id = LocaleR.string.clear_tests),
                                style = LocalTextStyle.current.copy(
                                    color = MaterialTheme.colorScheme.onSurface.onMediumEmphasis()
                                )
                            )
                        }
                    }
                }
            }

            items(
                items = testResults,
                key = { it.provider.id }
            ) { data ->
                TestResultCard(
                    isExpanded = viewModel.isExpanded(id = data.provider.id),
                    testResult = data,
                    onToggle = { viewModel.toggleCard(id = data.provider.id) },
                    showFullLog = { testCaseOutputToShow = data.provider to it },
                    modifier = Modifier.animateItem()
                )
            }
        }
    }

    if (viewModel.showRepetitiveTestWarning) {
        RepetitiveTestNoticeDialog(
            onSkip = {
                viewModel.startTests(
                    providers = args.providers,
                    skipTestedProviders = true
                )
            },
            onTestAgain = {
                viewModel.startTests(
                    providers = args.providers,
                    skipTestedProviders = false
                )
            },
            onDismiss = viewModel::hideRepetitiveTestWarning
        )
    }

    if (isSortingBottomSheetOpen) {
        SortBottomSheet(
            selectedSortOption = viewModel.sortOption,
            onSort = { viewModel.sortOption = it },
            onDismiss = { isSortingBottomSheetOpen = false }
        )
    }

    if (testCaseOutputToShow != null) {
        val (provider, testCaseOutput) = testCaseOutputToShow!!
        FullLogDialog(
            testCaseOutput = testCaseOutput,
            provider = provider,
            onDismiss = { testCaseOutputToShow = null }
        )
    }
}

private fun compareTestResults(
    a: ProviderTestResult,
    b: ProviderTestResult,
    sortOption: SortOption
): Int {
    val comparison = when (sortOption.sort) {
        SortOption.SortType.Name -> a.provider.name.compareTo(b.provider.name)
        SortOption.SortType.Date -> a.date.time.compareTo(b.date.time)
        SortOption.SortType.Score -> {
            val passedCountA = a.outputs.count { it.isSuccess }
            val passedCountB = b.outputs.count { it.isSuccess }
            passedCountA.compareTo(passedCountB)
        }
    }

    return if (sortOption.ascending) comparison else -comparison
}

@Preview
@Composable
private fun ProviderTestScreenPreview() {
    FlixclusiveTheme {
        Surface {
            ProviderTestScreen(
                navigator = object : GoBackAction {
                    override fun goBack() {}
                },
                args = ProviderTestScreenNavArgs(
                    arrayListOf(getDummyProviderData())
                )
            )
        }
    }
}
