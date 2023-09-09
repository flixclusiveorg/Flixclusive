package com.flixclusive.presentation.mobile.common

import com.ramcosta.composedestinations.annotation.NavGraph

@NavGraph(default = true)
annotation class MobileRootNavGraph(
    val start: Boolean = false
)