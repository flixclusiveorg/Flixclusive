package com.flixclusive.feature.mobile.provider.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.reflect.getDeclaredComposableMethod
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.navigation.deeplink.provider.ProviderDeepLinkConfig
import com.flixclusive.core.navigation.navigator.NavigateBack
import com.flixclusive.core.presentation.common.extensions.showToast
import com.flixclusive.core.presentation.common.util.DummyDataForPreview
import com.flixclusive.core.presentation.mobile.components.LoadingScreen
import com.flixclusive.core.presentation.mobile.components.material3.topbar.CommonTopBar
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.feature.mobile.provider.settings.components.ConditionalContent
import com.flixclusive.provider.ProviderPlugin
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.ExternalModuleGraph
import com.ramcosta.composedestinations.annotation.parameters.DeepLink

@Destination<ExternalModuleGraph>(
    navArgs = ProviderSettingsScreenNavArgs::class,
    deepLinks = [
        DeepLink(
            uriPattern = ProviderDeepLinkConfig.OPEN_SETTINGS_DEEP_LINK
        )
    ]
)
@Composable
internal fun ProviderSettingsScreen(
    navigator: NavigateBack,
    viewModel: ProviderSettingsScreenViewModel = hiltViewModel(),
) {
    val providerPlugin by viewModel.providerPlugin.collectAsStateWithLifecycle()

    ProviderSettingsScreenContent(
        navigator = navigator,
        provider = providerPlugin,
    )
}

@Composable
internal fun ProviderSettingsScreenContent(
    navigator: NavigateBack,
    provider: Async<ProviderPlugin>,
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            CommonTopBar(
                onNavigate = navigator::navigateBack,
                title = provider.let {
                    when (it) {
                        is Async.Success -> it.data.name
                        else -> ""
                    }
                },
            )
        },
    ) {
        AnimatedContent(
            targetState = provider,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier.padding(it)
        ) { state ->
            when (state) {
                is Async.Loading -> {
                    LoadingScreen()
                }

                is Async.Failure -> {
                    LaunchedEffect(true) {
                        context.showToast(state.message.asString(context))
                        navigator.navigateBack()
                    }
                }

                is Async.Success -> {
                    ProviderSettingsContent(
                        provider = state.data,
                        onGoBack = navigator::navigateBack,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProviderSettingsContent(
    provider: ProviderPlugin,
    onGoBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val method = remember(provider) {
        try {
            provider::class.java
                .getDeclaredComposableMethod("SettingsScreen")
        } catch (e: NoSuchMethodException) {
            null
        } catch (e: Exception) {
            null
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        ConditionalContent {
            val appResources = LocalResources.current
            val resources = remember { provider.resources ?: appResources }
            CompositionLocalProvider(LocalResources provides resources) {
                if (method != null) {
                    method.invoke(currentComposer, provider)
                } else {
                    LaunchedEffect(onGoBack) {
                        context.showToast(appResources.getString(R.string.provider_setting_not_found))
                        onGoBack()
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun ProviderSettingsScreenBasePreview() {
    val provider = remember {
        val plugin = object : ProviderPlugin() {
            @Composable
            override fun SettingsScreen() {
                val metadata = remember { DummyDataForPreview.getProviderMetadata() }

                Text("Settings Screen for ${metadata.name}")
            }
        }

        Async.Success(plugin)
    }

    FlixclusiveTheme {
        Surface {
            ProviderSettingsScreenContent(
                navigator = object : NavigateBack {
                    override fun navigateBack() {}
                },
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
