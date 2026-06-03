package com.flixclusive.domain.provider.usecase.links.impl

import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.data.provider.repository.MediaLinksRepository
import com.flixclusive.domain.provider.usecase.links.TestLinksProgress
import com.flixclusive.domain.provider.usecase.links.TestMediaLinksUseCase
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject

internal class TestMediaLinksUseCaseImpl @Inject constructor(
    private val mediaLinksRepository: MediaLinksRepository,
    private val okHttpClient: OkHttpClient,
    private val appDispatchers: AppDispatchers,
) : TestMediaLinksUseCase {
    override operator fun invoke(id: String) = flow {
        val entry = mediaLinksRepository.getLinksById(id) ?: return@flow
        val parentId = entry.cache.id
        val streams = entry.streams.filter { it.isValid }
        val total = streams.size

        var aliveCount = 0
        var deadCount = 0

        streams.forEachIndexed { index, stream ->
            emit(TestLinksProgress.Testing(url = stream.url, index = index + 1, total = total))

            val isAlive = try {
                withContext(appDispatchers.io) {
                    val requestBuilder = Request.Builder().url(stream.url).head()
                    stream.customHeaders?.forEach { (name, value) ->
                        requestBuilder.addHeader(name, value)
                    }
                    val response = okHttpClient.newCall(requestBuilder.build()).execute()
                    response.use { it.code in 200..399 }
                }
            } catch (_: Throwable) {
                false
            }

            if (isAlive) {
                mediaLinksRepository.markLinkAsAlive(stream.url, parentId)
                aliveCount++
            } else {
                mediaLinksRepository.markLinkAsDead(stream.url, parentId)
                deadCount++
            }
        }

        emit(TestLinksProgress.Done(aliveCount = aliveCount, deadCount = deadCount))
    }.flowOn(appDispatchers.io)
        .catch { e -> emit(TestLinksProgress.Error(e)) }
}
