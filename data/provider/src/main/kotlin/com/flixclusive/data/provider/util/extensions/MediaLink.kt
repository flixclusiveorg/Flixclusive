package com.flixclusive.data.provider.util.extensions

import com.flixclusive.core.database.entity.provider.DBStream
import com.flixclusive.core.database.entity.provider.DBSubtitle
import com.flixclusive.model.provider.link.Flag
import com.flixclusive.model.provider.link.Stream
import com.flixclusive.model.provider.link.Subtitle

fun Stream.toDBStream(parentId: String): DBStream {
    val expiresFlag = flags?.filterIsInstance<Flag.Expires>()?.firstOrNull()
    val requiresAuthFlag = flags?.filterIsInstance<Flag.RequiresAuth>()?.firstOrNull()
    val thirdPartyFlag = flags?.filterIsInstance<Flag.ThirdPartyGateway>()?.firstOrNull()

    return DBStream(
        url = url,
        parentId = parentId,
        label = name,
        description = description,
        expiresOn = expiresFlag?.expiresOn,
        customHeaders = requiresAuthFlag?.customHeaders?.takeIf { it.isNotEmpty() },
        isThirdPartyGateway = thirdPartyFlag != null,
        thirdPartyGatewayName = thirdPartyFlag?.name,
        thirdPartyGatewayLogo = thirdPartyFlag?.logo,
    )
}

fun Subtitle.toDBSubtitle(parentId: String): DBSubtitle {
    val requiresAuthFlag = flags?.filterIsInstance<Flag.RequiresAuth>()?.firstOrNull()

    return DBSubtitle(
        url = url,
        parentId = parentId,
        label = language,
        subtitleSource = type.name,
        customHeaders = requiresAuthFlag?.customHeaders?.takeIf { it.isNotEmpty() },
    )
}
