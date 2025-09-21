package com.flixclusive.feature.mobile.provider.settings

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.reflect.getDeclaredComposableMethod
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.flixclusive.core.navigation.navargs.ProviderMetadataNavArgs
import com.flixclusive.core.navigation.navigator.GoBackAction
import com.flixclusive.core.presentation.common.util.DummyDataForPreview
import com.flixclusive.core.presentation.mobile.components.topbar.CommonTopBar
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.feature.mobile.provider.settings.components.ConditionalContent
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.provider.Provider
import com.flixclusive.provider.util.res.LocalResources
import com.ramcosta.composedestinations.annotation.Destination
import okhttp3.OkHttpClient

@Destination(navArgsDelegate = ProviderMetadataNavArgs::class)
@Composable
internal fun ProviderSettingsScreen(
    navigator: GoBackAction,
    args: ProviderMetadataNavArgs,
    viewModel: ProviderSettingsScreenViewModel = hiltViewModel(),
) {
    ProviderSettingsScreenContent(
        navigator = navigator,
        metadata = args.metadata,
        provider = viewModel.providerInstance,
    )
}

@Composable
internal fun ProviderSettingsScreenContent(
    navigator: GoBackAction,
    metadata: ProviderMetadata,
    provider: Provider?,
) {
    Scaffold(
        topBar = {
            CommonTopBar(
                title = metadata.name,
                onNavigate = navigator::goBack,
            )
        },
    ) {
        Box(
            modifier = Modifier.padding(it),
            contentAlignment = Alignment.TopCenter,
        ) {
            if (provider != null) {
                // Need to call the composable with the reflection way bc
                // Compose won't let us call it the normal way.
                val method = remember {
                    provider::class.java
                        .getDeclaredComposableMethod("SettingsScreen")
                }

                ConditionalContent {
                    val resources = provider.resources ?: LocalContext.current.resources
                    CompositionLocalProvider(LocalResources provides resources) {
                        method.invoke(currentComposer, provider)
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun ProviderSettingsScreenBasePreview() {
    val metadata = remember { DummyDataForPreview.getDummyProviderMetadata() }
    val provider = remember {
        object : Provider() {
            override fun getApi(
                context: Context,
                client: OkHttpClient,
            ) = throw Error()

            @Composable
            override fun SettingsScreen() {
                Text("Settings Screen for ${metadata.name}")
            }
        }
    }

    FlixclusiveTheme {
        Surface {
            ProviderSettingsScreenContent(
                navigator = object : GoBackAction {
                    override fun goBack() {}
                },
                metadata = metadata,
                provider = provider,
            )
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun ProviderSettingsScreenCompactLandscapePreview() {
    ProviderSettingsScreenBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun ProviderSettingsScreenMediumPortraitPreview() {
    ProviderSettingsScreenBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun ProviderSettingsScreenMediumLandscapePreview() {
    ProviderSettingsScreenBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun ProviderSettingsScreenExtendedPortraitPreview() {
    ProviderSettingsScreenBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun ProviderSettingsScreenExtendedLandscapePreview() {
    ProviderSettingsScreenBasePreview()
}
