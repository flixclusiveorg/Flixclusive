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
import com.flixclusive.core.ui.common.CommonTopBar
import com.flixclusive.core.ui.common.CommonTopBarDefaults.DefaultTopBarHeight
import com.flixclusive.core.ui.common.navigation.navargs.ProviderMetadataNavArgs
import com.flixclusive.core.ui.common.navigation.navigator.GoBackAction
import com.flixclusive.core.ui.mobile.util.LocalGlobalScaffoldPadding
import com.flixclusive.provider.util.res.LocalResources
import com.ramcosta.composedestinations.annotation.Destination

@Destination(
    navArgsDelegate = ProviderMetadataNavArgs::class
)
@Composable
internal fun ProviderSettingsScreen(
    navigator: GoBackAction,
    args: ProviderMetadataNavArgs
) {
    val context = LocalContext.current
    val viewModel = hiltViewModel<ProviderSettingsScreenViewModel>()

    Box(
        modifier = Modifier
            .padding(LocalGlobalScaffoldPadding.current)
            .fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .statusBarsPadding()
                .padding(top = DefaultTopBarHeight)
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
            title = args.providerMetadata.name,
            onNavigate = navigator::goBack
        )
    }
}
