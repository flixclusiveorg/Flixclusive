package com.flixclusive.domain.provider.util.extensions

import com.flixclusive.core.database.entity.provider.InstalledRepository
import com.flixclusive.model.provider.Repository

internal fun Repository.toInstalledRepository(userId: Int) = InstalledRepository(
    url = url,
    name = name,
    owner = owner,
    rawLinkFormat = rawLinkFormat,
    userId = userId,
)
