package com.flixclusive.feature.mobile.repository

import androidx.compose.runtime.Composable
import com.flixclusive.core.ui.common.navigation.GoBackAction
import com.flixclusive.gradle.entities.Repository
import com.ramcosta.composedestinations.annotation.Destination

data class RepositoryScreenNavArgs(
    val repository: Repository
)

@Destination(
    navArgsDelegate = RepositoryScreenNavArgs::class
)
@Composable
fun RepositoryScreen(
    navigator: GoBackAction,
    args: RepositoryScreenNavArgs
) {

}