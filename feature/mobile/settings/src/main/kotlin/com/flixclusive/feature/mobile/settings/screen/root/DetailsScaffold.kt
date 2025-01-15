package com.flixclusive.feature.mobile.settings.screen.root

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.util.ifElse
import com.flixclusive.core.ui.mobile.component.topbar.CommonTopBarWithSearch
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            if (isDetailsVisible) {
                CommonTopBarWithSearch(
                    title = null,
                    isSearching = isSearching,
                    searchQuery = searchQuery.value,
                    onQueryChange = { searchQuery.value = it },
                    onToggleSearchBar = { isSearching = it },
                    onNavigateBack = navigateBack,
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
                        .padding(top = padding.calculateTopPadding())
                        .ifElse(
                            condition = isListAndDetailVisible,
                            ifTrueModifier =
                                Modifier
                                    .padding(UserScreenHorizontalPadding)
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
            DetailsScaffold(
                navigateBack = {},
                isListAndDetailVisible = false,
                isDetailsVisible = true,
                content = {},
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
