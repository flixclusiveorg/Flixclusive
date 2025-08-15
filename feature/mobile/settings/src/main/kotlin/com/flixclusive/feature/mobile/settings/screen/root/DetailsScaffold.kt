package com.flixclusive.feature.mobile.settings.screen.root

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.presentation.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.util.ifElse
import com.flixclusive.core.ui.mobile.component.topbar.CommonTopBarWithSearch
import com.flixclusive.feature.mobile.settings.TweakUI
import com.flixclusive.feature.mobile.settings.component.ClickableComponent
import com.flixclusive.feature.mobile.settings.util.LocalSettingsSearchQuery

@Composable
internal fun DetailsScaffold(
    navigateBack: () -> Unit,
    isListAndDetailVisible: Boolean,
    isDetailsVisible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = MaterialTheme.shapes.medium
    val surface = MaterialTheme.colorScheme.surface
    val brush =
        Brush.verticalGradient(
            0.6F to surface,
            1F to MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
        )

    var searchQuery = remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Scaffold(
        modifier = modifier
            .ifElse(
                condition = isDetailsVisible,
                ifTrueModifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            )
            .fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            if (isDetailsVisible) {
                CommonTopBarWithSearch(
                    title = null,
                    isSearching = isSearching,
                    searchQuery = { searchQuery.value },
                    onQueryChange = { searchQuery.value = it },
                    onToggleSearchBar = { isSearching = it },
                    onNavigate = navigateBack,
                    scrollBehavior = scrollBehavior,
                )
            }
        },
    ) { padding ->
        CompositionLocalProvider(
            LocalSettingsSearchQuery provides searchQuery,
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .ifElse(
                            condition = isListAndDetailVisible,
                            ifTrueModifier =
                            Modifier
                                .padding(UserScreenHorizontalPadding / 2)
                                .background(brush = brush, shape = shape),
                        ),
                content = content,
            )
        }
    }
}

@Preview
@Composable
private fun DetailsScaffoldBasePreview() {
    FlixclusiveTheme {
        Surface {
            val list = List(50) {
                TweakUI.ClickableTweak(
                    "A basic title for a clickable",
                    descriptionProvider = { "Some description" },
                    onClick = {}
                )
            }

            DetailsScaffold(
                navigateBack = {},
                isListAndDetailVisible = false,
                isDetailsVisible = true,
                content = {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(list) { item ->
                            ClickableComponent(
                                title = item.title,
                                descriptionProvider = item.descriptionProvider,
                                onClick = item.onClick
                            )
                        }
                    }
                },
            )
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun DetailsScaffoldCompactLandscapePreview() {
    DetailsScaffoldBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun DetailsScaffoldMediumPortraitPreview() {
    DetailsScaffoldBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun DetailsScaffoldMediumLandscapePreview() {
    DetailsScaffoldBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun DetailsScaffoldExtendedPortraitPreview() {
    DetailsScaffoldBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun DetailsScaffoldExtendedLandscapePreview() {
    DetailsScaffoldBasePreview()
}
