package com.flixclusive.provider.superstream

import com.flixclusive.core.util.coroutines.asyncCalls
import com.flixclusive.core.util.coroutines.mapAsync
import com.flixclusive.core.util.film.FilmType
import com.flixclusive.core.util.json.fromJson
import com.flixclusive.core.util.network.CryptographyUtil.base64Encode
import com.flixclusive.core.util.network.POST
import com.flixclusive.core.util.network.asString
import com.flixclusive.model.provider.SourceLink
import com.flixclusive.model.provider.Subtitle
import com.flixclusive.model.provider.SubtitleSource
import com.flixclusive.provider.base.Provider
import com.flixclusive.provider.base.dto.FilmInfo
import com.flixclusive.provider.base.dto.SearchResults
import com.flixclusive.provider.base.util.TvShowCacheData
import com.flixclusive.provider.superstream.SuperStreamCommon.apiUrl
import com.flixclusive.provider.superstream.SuperStreamCommon.appIdSecond
import com.flixclusive.provider.superstream.SuperStreamCommon.appKey
import com.flixclusive.provider.superstream.SuperStreamCommon.appVersion
import com.flixclusive.provider.superstream.SuperStreamCommon.appVersionCode
import com.flixclusive.provider.superstream.SuperStreamCommon.iv
import com.flixclusive.provider.superstream.SuperStreamCommon.key
import com.flixclusive.provider.superstream.SuperStreamCommon.secondApiUrl
import com.flixclusive.provider.superstream.dto.SuperStreamDownloadResponse
import com.flixclusive.provider.superstream.dto.SuperStreamMediaDetailResponse
import com.flixclusive.provider.superstream.dto.SuperStreamMediaDetailResponse.Companion.toMediaInfo
import com.flixclusive.provider.superstream.dto.SuperStreamSearchResponse
import com.flixclusive.provider.superstream.dto.SuperStreamSearchResponse.SuperStreamSearchItem.Companion.toSearchResultItem
import com.flixclusive.provider.superstream.dto.SuperStreamSubtitleResponse
import com.flixclusive.provider.superstream.dto.SuperStreamSubtitleResponse.SuperStreamSubtitleItem.Companion.toValidSubtitleFilePath
import com.flixclusive.provider.superstream.util.CipherUtils
import com.flixclusive.provider.superstream.util.CipherUtils.getVerify
import com.flixclusive.provider.superstream.util.MD5Utils
import com.flixclusive.provider.superstream.util.SuperStreamUtils.getExpiryDate
import com.flixclusive.provider.superstream.util.SuperStreamUtils.isError
import com.flixclusive.provider.superstream.util.SuperStreamUtils.randomToken
import okhttp3.Headers.Companion.toHeaders
import okhttp3.OkHttpClient

/**
 *
 * SuperStream = SS
 *
 * Based off:
 * https://github.com/movie-web/provider/tree/dev/src/provider/source/superstream
 * https://codeberg.org/cloudstream/cloudstream-extensions/src/branch/master/SuperStream/src/main/kotlin/com/lagradost/SuperStream.kt
 *
 * */
@Suppress("SpellCheckingInspection")
class SuperStream(
    client: OkHttpClient,
) : Provider(client) {
    override var name = "SuperStream"
    override val isMaintenance: Boolean = true

    private var tvCacheData = TvShowCacheData()

    private val headers = mapOf(
        "Platform" to "android",
        "Accept" to "charset=utf-8",
    )

    private inline fun <reified T : Any> requestCall(
        query: String,
        useAlternativeApi: Boolean = false,
    ): T? {
        val encryptedQuery = CipherUtils.encrypt(query, key, iv)!!
        val appKeyHash = MD5Utils.md5(appKey)!!
        val verify = getVerify(encryptedQuery, appKey, key)
        val newBody =
            """{"app_key":"$appKeyHash","verify":"$verify","encrypt_data":"$encryptedQuery"}"""

        val data = mapOf(
            "data" to base64Encode(newBody.toByteArray()),
            "appid" to "27",
            "platform" to "android",
            "version" to appVersionCode,
            // Probably best to randomize this
            "medium" to "Website",
            "token" to randomToken()
        )

        val errorMessage = "Failed to fetch SuperStream API"
        val url = if (useAlternativeApi) secondApiUrl else apiUrl
        val response = client.newCall(
            POST(
                url = url,
                data = data,
                headers = headers.toHeaders()
            )
        ).execute()

        val responseBody = response
            .body
            ?.charStream()
            ?.asString()
            ?: throw Exception(errorMessage + " [${response.code} - ${response.message}]")

        if(
            responseBody.contains(
                other = """"msg":"success""",
                ignoreCase = true
            ).not()
        ) throw Exception(errorMessage + " [${response.code} - ${response.message}]")

        if (response.isSuccessful && responseBody.isNotBlank())
            return fromJson(responseBody)
        else throw Exception("$errorMessage: [${responseBody}]")
    }

    override suspend fun search(query: String, page: Int, filmType: FilmType): SearchResults {
        val itemsPerPage = 20
        val apiQuery =
            // Originally 8 pagelimit
            """{"childmode":"0","app_version":"$appVersion","appid":"$appIdSecond","module":"Search4","channel":"Website","page":"$page","lang":"en","type":"all","keyword":"$query","pagelimit":"$itemsPerPage","expired_date":"${getExpiryDate()}","platform":"android"}"""

        val response = requestCall<SuperStreamSearchResponse>(apiQuery, true)

        val mappedItems = response?.data?.results?.map {
            it.toSearchResultItem()
        } ?: throw NullPointerException("Cannot search on SuperStream")

        return SearchResults(
            currentPage = page,
            results = mappedItems,
            hasNextPage = (page * itemsPerPage) < response.data.total
        )
    }

    override suspend fun getFilmInfo(
        filmId: String,
        filmType: FilmType,
    ): FilmInfo {
        if (tvCacheData.filmInfo?.id == filmId)
            return tvCacheData.filmInfo!!

        val apiQuery = if (filmType == FilmType.MOVIE) {
            """{"childmode":"0","uid":"","app_version":"$appVersion","appid":"$appIdSecond","module":"Movie_detail","channel":"Website","mid":"$filmId","lang":"en","expired_date":"${getExpiryDate()}","platform":"android","oss":"","group":""}"""
        } else {
            """{"childmode":"0","uid":"","app_version":"$appVersion","appid":"$appIdSecond","module":"TV_detail_1","display_all":"1","channel":"Website","lang":"en","expired_date":"${getExpiryDate()}","platform":"android","tid":"$filmId"}"""
        }

        val data = requestCall<SuperStreamMediaDetailResponse>(apiQuery)
        data?.msg?.isError("Failed to fetch movie info.")


        val result = data!!.toMediaInfo(filmType == FilmType.MOVIE)
        tvCacheData = TvShowCacheData(id = filmId, result)

        return result
    }

    override suspend fun getSourceLinks(
        filmId: String,
        season: Int?,
        episode: Int?,
        onLinkLoaded: (SourceLink) -> Unit,
        onSubtitleLoaded: (Subtitle) -> Unit,
    ) {
        val isMovie = season == null && episode == null

        val tvShowInfo = if(season != null) {
            getFilmInfo(filmId, FilmType.TV_SHOW)
        } else null

        val seasonToUse = if(
            tvShowInfo?.seasons != null
            && tvShowInfo.episodes != null
            && tvShowInfo.seasons!! <= season!!
            && tvShowInfo.episodes!! >= episode!!
        ) {
            tvShowInfo.seasons
        } else season

        val query = if (isMovie) {
            """{"childmode":"0","uid":"","app_version":"$appVersion","appid":"$appIdSecond","module":"Movie_downloadurl_v3","channel":"Website","mid":"$filmId","lang":"en","expired_date":"${getExpiryDate()}","platform":"android","oss":"1","group":""}"""
        } else {
            """{"childmode":"0","app_version":"$appVersion","module":"TV_downloadurl_v3","channel":"Website","episode":"$episode","expired_date":"${getExpiryDate()}","platform":"android","tid":"$filmId","oss":"1","uid":"","appid":"$appIdSecond","season":"$seasonToUse","lang":"en","group":""}"""
        }

        val downloadResponse = requestCall<SuperStreamDownloadResponse>(query, false)

        downloadResponse?.msg?.isError("Failed to fetch source.")

        println("downloadResponse = ${downloadResponse}")

        val data = downloadResponse?.data?.list?.find {
            it.path.isNullOrBlank().not()
        } ?: throw Exception("Cannot find source")


        // Should really run this query for every link :(
        val subtitleQuery = if (isMovie) {
            """{"childmode":"0","fid":"${data.fid}","uid":"","app_version":"$appVersion","appid":"$appIdSecond","module":"Movie_srt_list_v2","channel":"Website","mid":"$filmId","lang":"en","expired_date":"${getExpiryDate()}","platform":"android"}"""
        } else {
            """{"childmode":"0","fid":"${data.fid}","app_version":"$appVersion","module":"TV_srt_list_v2","channel":"Website","episode":"$episode","expired_date":"${getExpiryDate()}","platform":"android","tid":"$filmId","uid":"","appid":"$appIdSecond","season":"$seasonToUse","lang":"en"}"""
        }

        val subtitlesResponse = requestCall<SuperStreamSubtitleResponse>(subtitleQuery)
        subtitlesResponse?.msg?.isError("Failed to fetch subtitles.")

        asyncCalls(
            {
                subtitlesResponse?.data?.list?.mapAsync { subtitle ->
                    subtitle.subtitles
                        .sortedWith(compareByDescending { it.order })
                        .mapAsync {
                            if(
                                it.filePath != null
                                && it.lang != null
                            ) {
                                onSubtitleLoaded(
                                    Subtitle(
                                        language = "${it.language ?: "UNKNOWN"} [${it.lang}] - Votes: ${it.order}",
                                        url = it.filePath.toValidSubtitleFilePath(),
                                        type = SubtitleSource.ONLINE
                                    )
                                )
                            }
                        }
                }
            },
            {
                downloadResponse.data.list.mapAsync {
                    if(
                        !it.path.isNullOrBlank()
                        && !it.realQuality.isNullOrBlank()
                    ) {
                        onLinkLoaded(
                            SourceLink(
                                name = "${it.realQuality} server",
                                url = it.path
                            )
                        )
                    }
                }
            }
        )
    }
}
