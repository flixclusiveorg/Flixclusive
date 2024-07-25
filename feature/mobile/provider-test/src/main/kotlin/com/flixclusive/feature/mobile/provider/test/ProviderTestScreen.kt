package com.flixclusive.feature.mobile.provider.test

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.flixclusive.core.ui.common.navigation.GoBackAction
import com.flixclusive.gradle.entities.ProviderData
import com.ramcosta.composedestinations.annotation.Destination

data class ProviderTestScreenNavArgs(
    val providers: ArrayList<ProviderData>
)

@Destination(
    navArgsDelegate = ProviderTestScreenNavArgs::class
)
@Composable
fun ProviderTestScreen(
    navigator: GoBackAction,
    args: ProviderTestScreenNavArgs
) {
    val viewModel = hiltViewModel<ProviderTestScreenViewModel>()

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
//        TestScreenHeader()
    }
}
