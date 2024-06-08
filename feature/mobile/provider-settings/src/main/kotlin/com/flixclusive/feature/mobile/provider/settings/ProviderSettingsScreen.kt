package com.flixclusive.feature.mobile.provider.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flixclusive.core.ui.common.navigation.GoBackAction
import com.flixclusive.core.ui.mobile.util.isScrollingUp
import com.flixclusive.feature.mobile.provider.settings.component.ChangeLogsDialog
import com.flixclusive.feature.mobile.provider.settings.component.ProviderSettingsHeader
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

    var isChangeLogsDialogShown by remember { mutableStateOf(false) }

    val horizontalPadding = 10.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        ProviderSettingsTopBar(
            isVisible = shouldShowTopBar,
            repositoryUrl = args.providerData.repositoryUrl,
            changeLogs = args.providerData.changelog,
            openChangeLogs = { isChangeLogsDialogShown = true },
            onNavigationIconClick = navigator::goBack,
            modifier = Modifier.padding(horizontal = horizontalPadding)
        )

        ProviderSettingsHeader(
            providerData = args.providerData,
            modifier = Modifier.padding(horizontal = horizontalPadding)
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (viewModel.providerInstance != null) {
            // Need to call the composable with the reflection way bc
            // Compose won't let us call it the normal way.
            val method = remember {
                viewModel.providerInstance::class.java
                    .declaredMethods
                    .find {
                        it.name.equals("SettingsScreen")
                    }?.also {
                        it.isAccessible = true
                    }
            }

            method?.invoke(viewModel.providerInstance, currentComposer, 0)
        }
    }

    if (isChangeLogsDialogShown) {
        ChangeLogsDialog(
            changeLogs = args.providerData.changelog ?: "",
            changeLogsHeaderImage = args.providerData.changelogMedia,
            onDismiss = { isChangeLogsDialogShown = false }
        )
    }
}
