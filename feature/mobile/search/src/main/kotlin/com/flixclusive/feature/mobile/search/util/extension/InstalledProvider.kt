package com.flixclusive.feature.mobile.search.util.extension

import android.content.Context
import com.flixclusive.core.database.entity.provider.InstalledProvider
import com.flixclusive.model.provider.Language
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.model.provider.ProviderStatus
import com.flixclusive.model.provider.ProviderType
import com.flixclusive.core.strings.R as LocaleR

internal fun InstalledProvider.toFallbackProvider(context: Context): ProviderMetadata {
    return ProviderMetadata(
        id = id,
        name = id,
        repositoryUrl = repositoryUrl,
        buildUrl = "",
        versionName = "-1",
        versionCode = -1,
        language = Language.Multiple,
        providerType = ProviderType(context.getString(LocaleR.string.label_invalid)),
        status = ProviderStatus.Down,
    )
}
