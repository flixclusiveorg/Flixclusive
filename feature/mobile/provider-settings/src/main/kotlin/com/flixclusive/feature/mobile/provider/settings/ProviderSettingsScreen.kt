package com.flixclusive.feature.mobile.provider.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.flixclusive.core.ui.common.COMMON_TOP_BAR_HEIGHT
import com.flixclusive.core.ui.common.CommonTopBar
import com.flixclusive.core.ui.common.navigation.GoBackAction
import com.flixclusive.core.ui.common.navigation.navargs.ProviderInfoScreenNavArgs
import com.flixclusive.provider.util.res.LocalResources
import com.ramcosta.composedestinations.annotation.Destination

@Destination(
    navArgsDelegate = ProviderInfoScreenNavArgs::class
)
@Composable
internal fun ProviderSettingsScreen(
    navigator: GoBackAction,
    args: ProviderInfoScreenNavArgs
) {
    val context = LocalContext.current
    val viewModel = hiltViewModel<ProviderSettingsScreenViewModel>()

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .statusBarsPadding()
                .padding(top = COMMON_TOP_BAR_HEIGHT)
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

                CompositionLocalProvider(
                    LocalResources provides (viewModel.providerInstance.resources ?: context.resources)
                ) {
                    method?.invoke(viewModel.providerInstance, currentComposer, 0)
                }
            }
        }

        CommonTopBar(
            headerTitle = args.providerData.name,
            onNavigationIconClick = navigator::goBack
        )
    }
}
