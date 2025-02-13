package com.flixclusive.data.provider.util.extensions

import com.flixclusive.model.provider.link.Flag
import com.flixclusive.model.provider.link.Stream

internal fun List<Stream>.filterOutExpiredLinks(): List<Stream> {
    return filter {
        val expiryDate = it.flags?.getExpiredFlag()

        when {
            expiryDate == null -> return@filter true
            expiryDate.expiresOn < System.currentTimeMillis() -> return@filter false
            else -> true
        }
    }.toList()
}

private fun Set<Flag>.getExpiredFlag(): Flag.Expires?
    = filterIsInstance<Flag.Expires>().firstOrNull()
