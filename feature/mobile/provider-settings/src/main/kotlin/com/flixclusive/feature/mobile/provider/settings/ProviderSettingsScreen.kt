package com.flixclusive.feature.mobile.provider.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.flixclusive.core.ui.common.navigation.GoBackAction
import com.flixclusive.core.ui.common.navigation.ProviderInfoScreenNavArgs
import com.ramcosta.composedestinations.annotation.Destination

@Destination(
    navArgsDelegate = ProviderInfoScreenNavArgs::class
)
@Composable
fun ProviderSettingsScreen(
    navigator: GoBackAction,
    args: ProviderInfoScreenNavArgs
) {
    val viewModel = hiltViewModel<ProviderSettingsScreenViewModel>()

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .statusBarsPadding()
                .padding(top = TOP_BAR_HEIGHT)
        ) {
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

        ProviderSettingsTopBar(
            label = args.providerData.name,
            onNavigationIconClick = navigator::goBack
        )
    }
}
