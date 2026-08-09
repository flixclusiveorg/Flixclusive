package com.flixclusive.data.downloads.di

import javax.inject.Qualifier

/**
 * The [okhttp3.OkHttpClient] every download transfer shares.
 *
 * Distinct from the app-wide client because bulk transfers want no response cache (the payload is
 * being written to disk already) and a call timeout that actually bounds a probe or a chunk.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class DownloadHttpClient
