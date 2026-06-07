package com.flixclusive.domain.provider.usecase.links.impl

import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.database.entity.provider.DBMediaLink
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
    override operator fun invoke(links: List<DBMediaLink>) = flow {
        val total = links.size
        var aliveCount = 0
        var deadCount = 0

        links.forEachIndexed { index, link ->
            emit(TestLinksProgress.Testing(url = link.url, index = index + 1, total = total))

            val isAlive = try {
                withContext(appDispatchers.io) {
                    val requestBuilder = Request.Builder().url(link.url).head()
                    link.customHeaders?.forEach { (name, value) ->
                        requestBuilder.addHeader(name, value)
                    }
                    val response = okHttpClient.newCall(requestBuilder.build()).execute()
                    response.use { it.code in 200..399 }
                }
            } catch (_: Throwable) {
                false
            }

            if (isAlive) {
                mediaLinksRepository.markLinkAsAlive(link.url, link.parentId)
                aliveCount++
            } else {
                mediaLinksRepository.markLinkAsDead(link.url, link.parentId)
                deadCount++
            }
        }

        emit(TestLinksProgress.Done(aliveCount = aliveCount, deadCount = deadCount))
    }.flowOn(appDispatchers.io)
        .catch { e -> emit(TestLinksProgress.Error(e)) }
}
