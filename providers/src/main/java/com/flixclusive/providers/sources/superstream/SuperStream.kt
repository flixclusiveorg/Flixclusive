package com.flixclusive.providers.sources.superstream

import com.flixclusive.providers.interfaces.SourceProvider
import com.flixclusive.providers.models.common.MediaInfo
import com.flixclusive.providers.models.common.MediaType
import com.flixclusive.providers.models.common.SearchResults
import com.flixclusive.providers.models.common.SourceLink
import com.flixclusive.providers.models.common.Subtitle
import com.flixclusive.providers.models.providers.flixhq.TvShowCacheData
import com.flixclusive.providers.models.providers.superstream.SuperStreamDownloadResponse
import com.flixclusive.providers.models.providers.superstream.SuperStreamMediaDetailResponse
import com.flixclusive.providers.models.providers.superstream.SuperStreamMediaDetailResponse.Companion.toMediaInfo
import com.flixclusive.providers.models.providers.superstream.SuperStreamSearchResponse
import com.flixclusive.providers.models.providers.superstream.SuperStreamSearchResponse.SuperStreamSearchItem.Companion.toSearchResultItem
import com.flixclusive.providers.models.providers.superstream.SuperStreamSubtitleResponse
import com.flixclusive.providers.models.providers.superstream.SuperStreamSubtitleResponse.SuperStreamSubtitleItem.Companion.toValidSubtitleFilePath
import com.flixclusive.providers.sources.superstream.SuperStreamCommon.apiUrl
import com.flixclusive.providers.sources.superstream.SuperStreamCommon.appIdSecond
import com.flixclusive.providers.sources.superstream.SuperStreamCommon.appKey
import com.flixclusive.providers.sources.superstream.SuperStreamCommon.appVersion
import com.flixclusive.providers.sources.superstream.SuperStreamCommon.appVersionCode
import com.flixclusive.providers.sources.superstream.SuperStreamCommon.iv
import com.flixclusive.providers.sources.superstream.SuperStreamCommon.key
import com.flixclusive.providers.sources.superstream.SuperStreamCommon.secondApiUrl
import com.flixclusive.providers.sources.superstream.utils.CipherUtils
import com.flixclusive.providers.sources.superstream.utils.CipherUtils.getVerify
import com.flixclusive.providers.sources.superstream.utils.MD5Utils
import com.flixclusive.providers.sources.superstream.utils.SuperStreamUtils.getExpiryDate
import com.flixclusive.providers.sources.superstream.utils.SuperStreamUtils.isError
import com.flixclusive.providers.sources.superstream.utils.SuperStreamUtils.randomToken
import com.flixclusive.providers.utils.DecryptUtils.base64Encode
import com.flixclusive.providers.utils.JsonUtils.fromJson
import com.flixclusive.providers.utils.asyncCalls
import com.flixclusive.providers.utils.mapAsync
import com.flixclusive.providers.utils.network.OkHttpUtils.POST
import com.flixclusive.providers.utils.network.OkHttpUtils.asString
import okhttp3.Headers.Companion.toHeaders
import okhttp3.OkHttpClient

/**
 *
 * SuperStream = SS
 *
 * Based off:
 * https://github.com/movie-web/providers/tree/dev/src/providers/sources/superstream
 * https://codeberg.org/cloudstream/cloudstream-extensions/src/branch/master/SuperStream/src/main/kotlin/com/lagradost/SuperStream.kt
 *
 * */
@Suppress("SpellCheckingInspection")
class SuperStream(
    client: OkHttpClient,
) : SourceProvider(client) {
    override var name = "SuperStream"

    /**
     *
     * Cached data of previous called tv show.
     * This will ease rapid (tv) episodes requests from
     * the source/provider.
     *
     * */
    private var tvCacheData: TvShowCacheData = TvShowCacheData()

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
        val response = client.newCall(POST(
            url = url,
            data = data,
            headers = headers.toHeaders()
        )).execute()

        val responseBody = response
            .body
            ?.charStream()
            ?.asString()
            ?: throw Exception(errorMessage)

        if(
            responseBody.contains(
                other = """"msg":"success""",
                ignoreCase = true
            ).not()
        ) throw Exception(errorMessage)

        if (response.isSuccessful && responseBody.isNotBlank())
            return fromJson(responseBody)
        else throw Exception("$errorMessage: [${response.body.toString()}]")
    }

    override suspend fun search(query: String, page: Int, mediaType: MediaType): SearchResults {
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

    override suspend fun getMediaInfo(mediaId: String, mediaType: MediaType): MediaInfo {
        if (tvCacheData.mediaInfo?.id == mediaId)
            return tvCacheData.mediaInfo!!

        val apiQuery = if (mediaType == MediaType.Movie) {
            """{"childmode":"0","uid":"","app_version":"$appVersion","appid":"$appIdSecond","module":"Movie_detail","channel":"Website","mid":"$mediaId","lang":"en","expired_date":"${getExpiryDate()}","platform":"android","oss":"","group":""}"""
        } else {
            """{"childmode":"0","uid":"","app_version":"$appVersion","appid":"$appIdSecond","module":"TV_detail_1","display_all":"1","channel":"Website","lang":"en","expired_date":"${getExpiryDate()}","platform":"android","tid":"$mediaId"}"""
        }

        val data = requestCall<SuperStreamMediaDetailResponse>(apiQuery)
        data?.msg?.isError("Failed to fetch movie info.")


        val result = data!!.toMediaInfo(mediaType == MediaType.Movie)
        tvCacheData = TvShowCacheData(id = mediaId, result)

        return result
    }

    override suspend fun getSourceLinks(
        mediaId: String,
        season: Int?,
        episode: Int?,
        onLinkLoaded: (SourceLink) -> Unit,
        onSubtitleLoaded: (Subtitle) -> Unit,
    ) {
        val isMovie = season == null && episode == null

        val tvShowInfo = if(season != null) {
            getMediaInfo(mediaId, MediaType.TvShow)
        } else null

        val seasonToUse = if(
            tvShowInfo?.seasons != null
            && tvShowInfo.episodes != null
            && tvShowInfo.seasons <= season!!
            && tvShowInfo.episodes >= episode!!
        ) {
            tvShowInfo.seasons
        } else season

        val query = if (isMovie) {
            """{"childmode":"0","uid":"","app_version":"$appVersion","appid":"$appIdSecond","module":"Movie_downloadurl_v3","channel":"Website","mid":"$mediaId","lang":"en","expired_date":"${getExpiryDate()}","platform":"android","oss":"1","group":""}"""
        } else {
            """{"childmode":"0","app_version":"$appVersion","module":"TV_downloadurl_v3","channel":"Website","episode":"$episode","expired_date":"${getExpiryDate()}","platform":"android","tid":"$mediaId","oss":"1","uid":"","appid":"$appIdSecond","season":"$seasonToUse","lang":"en","group":""}"""
        }

        val downloadResponse = requestCall<SuperStreamDownloadResponse>(query, false)

        downloadResponse?.msg?.isError("Failed to fetch source.")

        val data = downloadResponse?.data?.list?.find {
            it.path.isNullOrBlank().not()
        } ?: throw Exception("Cannot find source")

        // Should really run this query for every link :(
        val subtitleQuery = if (isMovie) {
            """{"childmode":"0","fid":"${data.fid}","uid":"","app_version":"$appVersion","appid":"$appIdSecond","module":"Movie_srt_list_v2","channel":"Website","mid":"$mediaId","lang":"en","expired_date":"${getExpiryDate()}","platform":"android"}"""
        } else {
            """{"childmode":"0","fid":"${data.fid}","app_version":"$appVersion","module":"TV_srt_list_v2","channel":"Website","episode":"$episode","expired_date":"${getExpiryDate()}","platform":"android","tid":"$mediaId","uid":"","appid":"$appIdSecond","season":"$seasonToUse","lang":"en"}"""
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
                                        lang = "${it.language ?: "UNKNOWN"} [${it.lang}] - Votes: ${it.order}",
                                        url = it.filePath.toValidSubtitleFilePath()
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
