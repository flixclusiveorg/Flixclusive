package com.flixclusive.domain.provider.testing.impl

import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.core.util.log.infoLog
import com.flixclusive.domain.provider.R
import com.flixclusive.domain.provider.testing.model.ProviderTestCaseResult
import com.flixclusive.domain.provider.testing.model.TestStatus
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
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.junit.Assert.assertNotNull
import kotlin.time.measureTimedValue

@Suppress("MemberVisibilityCanBePrivate")
internal class TestCases(
    private val appDispatchers: AppDispatchers,
) {
    private data class AssertionResult<T>(
        val status: TestStatus,
        val data: T? = null,
        val error: Throwable? = null,
    )

    val propertyTestCases =
        listOf(
            ProviderTestCase(
                name = UiText.from(R.string.ptest_get_testable_film),
                stopTestOnFailure = true,
                test = ::testFilmIsSafeToGet,
            ),
            ProviderTestCase(
                name = UiText.from(R.string.ptest_get_catalog_list),
                test = ::catalogListIsSafeToGet,
            ),
            ProviderTestCase(
                name = UiText.from(R.string.ptest_get_search_filters),
                test = ::filtersAreSafeToGet,
            ),
        )

    val methodTestCases =
        listOf(
            ProviderTestCase(
                name = UiText.from(R.string.ptest_method_get_catalog_items),
                test = ::methodGetCatalogItemsIsSafeToCall,
            ),
            ProviderTestCase(
                name = UiText.from(R.string.ptest_method_get_search_items),
                test = ::methodSearchIsSafeToCall,
            ),
            ProviderTestCase(
                name = UiText.from(R.string.ptest_method_get_film_details),
                test = ::methodGetFilmMetadataIsSafeToCall,
            ),
            ProviderTestCase(
                name = UiText.from(R.string.ptest_method_get_links),
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

    /**
     * Data class representing a test case for a provider.
     * */
    data class ProviderTestCase(
        val name: UiText,
        val stopTestOnFailure: Boolean = false,
        val test: suspend (testName: UiText, api: ProviderApi) -> ProviderTestCaseResult,
    )

    /**
     * Asserts that a given property is not null and logs the assertion.
     * */
    private fun <T> assertProperty(data: T) {
        infoLog("Asserting property: $data")
        assertNotNull(data)
        assert(true)
    }

    /**
     * Asserts the result of a test case while measuring the time taken to execute it.
     *
     * @param testName The name of the test case.
     * @param successShortLog A lambda that generates a short log message for a successful test case.
     * @param successFullLog A lambda that generates a full log message for a successful test case.
     * @param failedShortLog A lambda that generates a short log message for a failed test case.
     * @param notImplementedShortLog An optional lambda that generates a short log message for a not implemented test case.
     * @param failedFullLog An optional lambda that generates a full log message for a failed test case.
     * @param assert A suspend function that performs the actual test and returns a result of type T.
     *
     * @return A [ProviderTestCaseResult] containing the result of the test case, including status, time taken, and log messages.
     * */
    private suspend fun <T> assertWithMeasuredTime(
        testName: UiText,
        successShortLog: (T?) -> UiText,
        successFullLog: (T?) -> UiText,
        failedShortLog: (Throwable?) -> UiText,
        notImplementedShortLog: ((Throwable?) -> UiText)? = null,
        failedFullLog: ((Throwable?) -> UiText)? = null,
        assert: suspend () -> T?,
    ): ProviderTestCaseResult {
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
                TestStatus.RUNNING -> UiText.from("") to UiText.from("")
                TestStatus.NOT_IMPLEMENTED, TestStatus.FAILURE -> {
                    val actualNotImplementedShortLog =
                        safeCall { notImplementedShortLog?.invoke(value.error) }
                            ?: value.error?.localizedMessage?.let(UiText::from)
                            ?: UiText.from(R.string.ptest_error_not_implemented)

                    val actualFailedFullLog =
                        safeCall { failedFullLog?.invoke(value.error) }
                            ?: value.error?.stackTraceToString()?.let(UiText::from)
                            ?: UiText.from(R.string.ptest_error_default_full_log)

                    val shortLog =
                        safeCall { failedShortLog(value.error) }
                            ?: value.error?.localizedMessage?.let(UiText::from)
                            ?: UiText.from(R.string.ptest_error_default_short_log)

                    if (value.status == TestStatus.NOT_IMPLEMENTED) {
                        actualNotImplementedShortLog to actualFailedFullLog
                    } else {
                        shortLog to actualFailedFullLog
                    }
                }
            }

        return ProviderTestCaseResult(
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

        return UiText.from(json.encodeToString(films ?: emptyList()))
    }

    private fun getShortLogForItemsFound(size: Int?): UiText {
        if (size == null || size == 0) {
            return UiText.from(R.string.ptest_success_items_count_found_empty)
        }

        return UiText.from(R.string.ptest_success_items_count_found, size)
    }

    suspend fun testFilmIsSafeToGet(
        testName: UiText,
        api: ProviderApi,
    ): ProviderTestCaseResult {
        return assertWithMeasuredTime(
            testName = testName,
            failedShortLog = { UiText.from(R.string.ptest_error_get_testable_film) },
            successShortLog = { UiText.from(api.testFilm.title) },
            successFullLog = { UiText.from(json.encodeToString(api.testFilm)) },
            assert = { assertProperty(api.testFilm) },
        )
    }

    suspend fun catalogListIsSafeToGet(
        testName: UiText,
        api: ProviderApi,
    ): ProviderTestCaseResult {
        return assertWithMeasuredTime(
            testName = testName,
            failedShortLog = { UiText.from(R.string.ptest_error_get_catalog_list) },
            successShortLog = { getShortLogForItemsFound(api.catalogs.size) },
            successFullLog = { UiText.from(json.encodeToString(api.catalogs)) },
            assert = { assertProperty(api.catalogs) },
        )
    }

    suspend fun filtersAreSafeToGet(
        testName: UiText,
        api: ProviderApi,
    ): ProviderTestCaseResult {
        return assertWithMeasuredTime(
            testName = testName,
            failedShortLog = { UiText.from(R.string.ptest_error_get_search_filters) },
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

                UiText.from(json.encodeToString(mapOfFilters))
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
    ): ProviderTestCaseResult {
        return assertWithMeasuredTime(
            testName = testName,
            failedShortLog = {
                val catalogTested =
                    if (api.catalogs.isEmpty()) {
                        ""
                    } else {
                        "- ${api.catalogs.first().name}"
                    }

                UiText.from(R.string.ptest_error_method_get_catalog_items, catalogTested)
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
    ): ProviderTestCaseResult {
        return assertWithMeasuredTime(
            testName = testName,
            failedShortLog = {
                val catalogTested =
                    if (api.catalogs.isEmpty()) {
                        ""
                    } else {
                        "- ${api.catalogs.first().name}"
                    }

                UiText.from(R.string.ptest_error_method_get_catalog_items, catalogTested)
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
    ): ProviderTestCaseResult {
        return assertWithMeasuredTime(
            testName = testName,
            failedShortLog = {
                UiText.from(
                    id = R.string.ptest_error_method_get_film_details,
                    "${api.testFilm.title} [${api.testFilm.identifier}]",
                )
            },
            successShortLog = { UiText.from(it?.title ?: api.testFilm.title) },
            successFullLog = { UiText.from(json.encodeToString(it)) },
            assert = { api.getMetadata(api.testFilm) },
        )
    }

    suspend fun methodGetLinksIsSafeToCall(
        testName: UiText,
        api: ProviderApi,
    ): ProviderTestCaseResult {
        return assertWithMeasuredTime(
            testName = testName,
            failedShortLog = {
                UiText.from(
                    id = R.string.ptest_error_method_get_links,
                    "${api.testFilm.title} [${api.testFilm.identifier}]",
                )
            },
            successShortLog = { getShortLogForItemsFound(it?.size) },
            successFullLog = { links ->
                val linksOnly = links?.map { it.url }

                UiText.from(json.encodeToString(linksOnly))
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
                        withContext(appDispatchers.main) {
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
