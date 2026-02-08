package com.flixclusive.feature.mobile.player.preview

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import com.flixclusive.core.presentation.player.AppDataSourceFactory

@OptIn(UnstableApi::class)
internal class PreviewDataSourceFactory(
    private val context: Context
) : AppDataSourceFactory {
    override val local get() = DefaultDataSource.Factory(context)
    override val remote get() = DefaultHttpDataSource.Factory()

    override fun setRequestProperties(properties: Map<String, String>) {
        // No-op for preview
    }
}
