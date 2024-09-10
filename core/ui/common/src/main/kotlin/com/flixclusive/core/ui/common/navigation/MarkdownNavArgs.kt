package com.flixclusive.core.ui.common.navigation

data class MarkdownNavArgs(
    val title: String,
    val description: String,
)

interface MarkdownNavigator {
    fun openMarkdownScreen(
        title: String,
        description: String
    )
}