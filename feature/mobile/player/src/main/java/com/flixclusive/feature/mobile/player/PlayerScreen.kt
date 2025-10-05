package com.flixclusive.feature.mobile.player

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination

@Destination(
    navArgsDelegate = PlayerScreenNavArgs::class,
)
@Composable
internal fun PlayerScreen(
    navigator: PlayerScreenNavigator,
    args: PlayerScreenNavArgs,
    viewModel: PlayerScreenViewModel = hiltViewModel(),
) {

}
