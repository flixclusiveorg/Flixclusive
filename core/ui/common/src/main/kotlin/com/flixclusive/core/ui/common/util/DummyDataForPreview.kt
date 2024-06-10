package com.flixclusive.core.ui.common.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.flixclusive.gradle.entities.Author
import com.flixclusive.gradle.entities.Language
import com.flixclusive.gradle.entities.ProviderData
import com.flixclusive.gradle.entities.ProviderType
import com.flixclusive.gradle.entities.Status

object DummyDataForPreview {
    @Composable
    fun getDummyProviderData() = remember {
        ProviderData(
            authors = List(5) { Author("FLX $it") },
            repositoryUrl = "https://github.com/flixclusive/123Movies",
            buildUrl = "https://raw.githubusercontent.com/Flixclusive/plugins-template/builds/updater.json",
            changelog = """Test""",
            versionName = "1.0.0",
            versionCode = 10000,
            description = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
            iconUrl = null,
            language = Language.Multiple,
            name = "CineFlix",
            providerType = ProviderType.All,
            status = Status.Working
        )
    }
}