package com.flixclusive.domain.provider.test

import com.flixclusive.core.locale.UiText
import com.flixclusive.core.locale.util.toUiText
import com.flixclusive.core.util.coroutines.AppDispatchers.Companion.withMainContext
import com.flixclusive.core.util.log.infoLog
import com.flixclusive.model.film.Film
import com.flixclusive.model.film.FilmMetadata
import com.flixclusive.model.film.FilmSearchItem
import com.flixclusive.model.film.Movie
import com.flixclusive.model.film.TvShow
import com.flixclusive.model.film.common.tv.Episode
import com.flixclusive.model.film.util.FilmType
import com.flixclusive.model.provider.link.MediaLink
import com.flixclusive.provider.ProviderApi
import com.flixclusive.provider.ProviderWebViewApi
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.junit.Assert.assertNotNull
import kotlin.time.measureTimedValue
import com.flixclusive.core.locale.R as LocaleR

@Suppress("MemberVisibilityCanBePrivate")
internal object ProviderTestCases {
    val propertyTestCases =
        listOf(
            ProviderTestCase(
                name = LocaleR.string.ptest_get_testable_film.toUiText(),
                stopTestOnFailure = true,
                test = ::testFilmIsSafeToGet,
            ),
            ProviderTestCase(
                name = LocaleR.string.ptest_get_catalog_list.toUiText(),
                test = ::catalogListIsSafeToGet,
            ),
            ProviderTestCase(
                name = LocaleR.string.ptest_get_search_filters.toUiText(),
                test = ::filtersAreSafeToGet,
            ),
        )

    val methodTestCases =
        listOf(
            ProviderTestCase(
                name = LocaleR.string.ptest_method_get_catalog_items.toUiText(),
                test = ::methodGetCatalogItemsIsSafeToCall,
            ),
            ProviderTestCase(
                name = LocaleR.string.ptest_method_get_search_items.toUiText(),
                test = ::methodSearchIsSafeToCall,
            ),
            ProviderTestCase(
                name = LocaleR.string.ptest_method_get_film_details.toUiText(),
                test = ::methodGetFilmMetadataIsSafeToCall,
            ),
            ProviderTestCase(
                name = LocaleR.string.ptest_method_get_links.toUiText(),
                test = ::methodGetLinksIsSafeToCall,
            ),
        )

    private val module =
        SerializersModule {
            polymorphic(Film::class) {
                subclass(FilmSearchItem::class, FilmSearchItem.serializer())
            }

            polymorphic(FilmMetadata::class) {
                subclass(Movie::class, Movie.serializer())
                subclass(TvShow::class, TvShow.serializer())
            }
        }

    /** For pretty printing */
    @OptIn(ExperimentalSerializationApi::class)
    private val json =
        Json {
            prettyPrint = true
            prettyPrintIndent = "  "
            serializersModule = module
        }

    data class ProviderTestCase(
        val name: UiText,
        val stopTestOnFailure: Boolean = false,
        val test: suspend (testName: UiText, api: ProviderApi) -> ProviderTestCaseOutput,
    )

    private fun <T> assertProperty(data: T) {
        infoLog("Asserting property: $data")
        assertNotNull(data)
        assert(true)
    }

    private suspend fun <T> assertWithMeasuredTime(
        testName: UiText,
        successShortLog: (T?) -> UiText,
        successFullLog: (T?) -> UiText,
        failedShortLog: (Throwable?) -> UiText,
        notImplementedShortLog: ((Throwable?) -> UiText)? = null,
        failedFullLog: ((Throwable?) -> UiText)? = null,
        assert: suspend () -> T?,
    ): ProviderTestCaseOutput {
        val (value, timeTaken) =
            measureTimedValue {
                try {
                    val data = assert()
                    AssertionResult(
                        status = TestStatus.SUCCESS,
                        data = data,
                    )
                } catch (e: Throwable) {
                    val status =
                        when (e) {
                            is NotImplementedError -> TestStatus.NOT_IMPLEMENTED
                            else -> TestStatus.FAILURE
                        }

                    AssertionResult(
                        status = status,
                        error = e,
                    )
                }
            }

        val (shortLog, fullLog) =
            when (value.status) {
                TestStatus.SUCCESS -> successShortLog(value.data) to successFullLog(value.data)
                TestStatus.RUNNING -> UiText.StringValue("") to UiText.StringValue("")
                TestStatus.NOT_IMPLEMENTED, TestStatus.FAILURE -> {
                    val actualNotImplementedShortLog =
                        notImplementedShortLog?.invoke(value.error)
                            ?: value.error?.localizedMessage?.toUiText()
                            ?: LocaleR.string.ptest_error_not_implemented.toUiText()

                    val actualFailedFullLog =
                        failedFullLog?.invoke(value.error)
                            ?: value.error!!.stackTraceToString().toUiText()

                    if (value.status == TestStatus.NOT_IMPLEMENTED) {
                        actualNotImplementedShortLog to actualFailedFullLog
                    } else {
                        failedShortLog(value.error) to actualFailedFullLog
                    }
                }
            }

        return ProviderTestCaseOutput(
            status = value.status,
            name = testName,
            timeTaken = timeTaken,
            shortLog = shortLog,
            fullLog = fullLog,
        )
    }

    private fun List<Film>?.filmsResponseToFullLog(): UiText {
        val films =
            this?.map {
                "${it.title} [${it.identifier}]"
            }

        return json.encodeToString(films ?: emptyList()).toUiText()
    }

    private fun getShortLogForItemsFound(size: Int?): UiText {
        if (size == null || size == 0) {
            return LocaleR.string.ptest_success_items_count_found_empty.toUiText()
        }

        return LocaleR.string.ptest_success_items_count_found.toUiText(size)
    }

    suspend fun testFilmIsSafeToGet(
        testName: UiText,
        api: ProviderApi,
    ): ProviderTestCaseOutput {
        return assertWithMeasuredTime(
            testName = testName,
            failedShortLog = { LocaleR.string.ptest_error_get_testable_film.toUiText() },
            successShortLog = { api.testFilm.title.toUiText() },
            successFullLog = { json.encodeToString(api.testFilm).toUiText() },
            assert = { assertProperty(api.testFilm) },
        )
    }

    suspend fun catalogListIsSafeToGet(
        testName: UiText,
        api: ProviderApi,
    ): ProviderTestCaseOutput {
        return assertWithMeasuredTime(
            testName = testName,
            failedShortLog = { LocaleR.string.ptest_error_get_catalog_list.toUiText() },
            successShortLog = { getShortLogForItemsFound(api.catalogs.size) },
            successFullLog = { json.encodeToString(api.catalogs).toUiText() },
            assert = { assertProperty(api.catalogs) },
        )
    }

    suspend fun filtersAreSafeToGet(
        testName: UiText,
        api: ProviderApi,
    ): ProviderTestCaseOutput {
        return assertWithMeasuredTime(
            testName = testName,
            failedShortLog = { LocaleR.string.ptest_error_get_search_filters.toUiText() },
            successShortLog = { getShortLogForItemsFound(api.filters.size) },
            successFullLog = {
                val mapOfFilters =
                    api.filters.withIndex().associate { (index, list) ->
                        val key = list.firstOrNull()?.name ?: "Filter #${index + 1}"
                        val value =
                            list.map {
                                mapOf(
                                    "Name" to it.name,
                                    "Default value" to it.state.toString(),
                                )
                            }

                        key to value
                    }

                json.encodeToString(mapOfFilters).toUiText()
            },
            assert = {
                api.filters.forEach { filterGroup ->
                    filterGroup.forEach { filter ->
                        assertProperty(filter)
                    }
                }
            },
        )
    }

    suspend fun methodGetCatalogItemsIsSafeToCall(
        testName: UiText,
        api: ProviderApi,
    ): ProviderTestCaseOutput {
        return assertWithMeasuredTime(
            testName = testName,
            failedShortLog = {
                val catalogTested =
                    if (api.catalogs.isEmpty()) {
                        ""
                    } else {
                        "- ${api.catalogs.first().name}"
                    }

                LocaleR.string.ptest_error_method_get_catalog_items.toUiText(catalogTested)
            },
            successShortLog = { getShortLogForItemsFound(it?.size) },
            successFullLog = { it.filmsResponseToFullLog() },
            assert = {
                if (api.catalogs.isEmpty()) {
                    throw NotImplementedError("Provider catalogs are empty")
                }

                val response =
                    api.getCatalogItems(
                        catalog = api.catalogs.first(),
                    )

                response.results
            },
        )
    }

    suspend fun methodSearchIsSafeToCall(
        testName: UiText,
        api: ProviderApi,
    ): ProviderTestCaseOutput {
        return assertWithMeasuredTime(
            testName = testName,
            failedShortLog = {
                val catalogTested =
                    if (api.catalogs.isEmpty()) {
                        ""
                    } else {
                        "- ${api.catalogs.first().name}"
                    }

                LocaleR.string.ptest_error_method_get_catalog_items.toUiText(catalogTested)
            },
            successShortLog = { getShortLogForItemsFound(it?.size) },
            successFullLog = { it.filmsResponseToFullLog() },
            assert = {
                val response =
                    with(api.testFilm) {
                        api.search(
                            title = title,
                            id = id,
                            imdbId = imdbId,
                            tmdbId = tmdbId,
                        )
                    }

                response.results
            },
        )
    }

    suspend fun methodGetFilmMetadataIsSafeToCall(
        testName: UiText,
        api: ProviderApi,
    ): ProviderTestCaseOutput {
        return assertWithMeasuredTime(
            testName = testName,
            failedShortLog = {
                LocaleR.string.ptest_error_method_get_film_details.toUiText(
                    "${api.testFilm.title} [${api.testFilm.identifier}]",
                )
            },
            successShortLog = { (it?.title ?: api.testFilm.title).toUiText() },
            successFullLog = { json.encodeToString(it).toUiText() },
            assert = { api.getMetadata(api.testFilm) },
        )
    }

    suspend fun methodGetLinksIsSafeToCall(
        testName: UiText,
        api: ProviderApi,
    ): ProviderTestCaseOutput {
        return assertWithMeasuredTime(
            testName = testName,
            failedShortLog = {
                LocaleR.string.ptest_error_method_get_links.toUiText(
                    "${api.testFilm.title} [${api.testFilm.identifier}]",
                )
            },
            successShortLog = { getShortLogForItemsFound(it?.size) },
            successFullLog = { links ->
                val linksOnly = links?.map { it.url }

                json.encodeToString(linksOnly).toUiText()
            },
            assert = {
                val episodeData =
                    when (api.testFilm.filmType) {
                        FilmType.TV_SHOW -> Episode(number = 1, season = 1)
                        else -> null
                    }

                val links = mutableSetOf<MediaLink>()
                if (api is ProviderWebViewApi) {
                    val webView =
                        withMainContext {
                            api.getWebView()
                        }

                    webView.getLinks(
                        watchId = api.testFilm.id ?: api.testFilm.identifier,
                        film = api.testFilm,
                        episode = episodeData,
                        onLinkFound = links::add,
                    )
                } else {
                    api.getLinks(
                        watchId = api.testFilm.id ?: api.testFilm.identifier,
                        film = api.testFilm,
                        episode = episodeData,
                        onLinkFound = links::add,
                    )
                }

                return@assertWithMeasuredTime links
            },
        )
    }
}
