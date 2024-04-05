package com.flixclusive.feature.mobile.provider.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flixclusive.core.ui.common.navigation.GoBackAction
import com.flixclusive.core.ui.mobile.util.isScrollingUp
import com.flixclusive.feature.mobile.provider.settings.component.ProviderSettingsTopBar
import com.flixclusive.gradle.entities.ProviderData
import com.ramcosta.composedestinations.annotation.Destination

data class ProviderSettingsScreenNavArgs(
    val providerData: ProviderData
)

@Destination(
    navArgsDelegate = ProviderSettingsScreenNavArgs::class
)
@Composable
fun ProviderSettingsScreen(
    navigator: GoBackAction,
    args: ProviderSettingsScreenNavArgs
) {
    val viewModel = hiltViewModel<ProviderSettingsScreenViewModel>()

    val listState = rememberLazyListState()
    val shouldShowTopBar by listState.isScrollingUp()

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            ProviderSettingsTopBar(
                isVisible = shouldShowTopBar,
                repositoryUrl = args.providerData.repositoryUrl,
                onNavigationIconClick = navigator::goBack
            )
        }
    ) {
        Box(
            modifier = Modifier
                .padding(it)
                .fillMaxSize()
        ) {
            viewModel.providerInstance?.SettingsScreen()
        }
    }
}
