package com.flixclusive.feature.mobile.library.details

import com.flixclusive.core.database.entity.library.LibraryList
import com.flixclusive.model.provider.ProviderMetadata

data class LibraryDetailsNavArgs(
    val library: LibraryList,
    val tracker: ProviderMetadata? = null,
)
