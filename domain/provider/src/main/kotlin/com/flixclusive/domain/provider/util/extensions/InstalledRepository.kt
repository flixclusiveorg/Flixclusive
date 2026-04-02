package com.flixclusive.domain.provider.util.extensions

import com.flixclusive.core.database.entity.provider.InstalledRepository
import com.flixclusive.model.provider.Repository

fun InstalledRepository.toRepository(): Repository {
    return Repository(
        owner = owner,
        name = name,
        url = url,
        rawLinkFormat = rawLinkFormat,
    )
}
