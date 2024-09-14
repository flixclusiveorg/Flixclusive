package com.flixclusive.domain.provider.test

import com.flixclusive.core.util.coroutines.AppDispatchers.Companion.withMainContext
import com.flixclusive.core.locale.UiText
import com.flixclusive.model.film.util.FilmType
import com.flixclusive.core.util.log.infoLog
import com.flixclusive.domain.provider.util.StringHelper.createString
import com.flixclusive.domain.provider.util.StringHelper.getString
import com.flixclusive.model.provider.link.MediaLink
import com.flixclusive.model.film.Film
import com.flixclusive.model.film.FilmDetails
import com.flixclusive.model.film.FilmSearchItem
import com.flixclusive.model.film.Movie
import com.flixclusive.model.film.TvShow
import com.flixclusive.model.film.common.tv.Episode
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
    val propertyTestCases = listOf(
        ProviderTestCase(
            name = getString(LocaleR.string.ptest_get_testable_film),
            stopTestOnFailure = true,
            test = ::testFilmIsSafeToGet,
        ),
        ProviderTestCase(
            name = getString(LocaleR.string.ptest_get_catalog_list),
            test = ::catalogListIsSafeToGet,
        ),
        ProviderTestCase(
            name = getString(LocaleR.string.ptest_get_search_filters),
            test = ::filtersAreSafeToGet,
        ),
    )

    val methodTestCases = listOf(
        ProviderTestCase(
            name = getString(LocaleR.string.ptest_method_get_catalog_items),
            test = ::methodGetCatalogItemsIsSafeToCall,
        ),
        ProviderTestCase(
            name = getString(LocaleR.string.ptest_method_get_search_items),
            test = ::methodSearchIsSafeToCall,
        ),
        ProviderTestCase(
            name = getString(LocaleR.string.ptest_method_get_film_details),
            test = ::methodGetFilmDetailsIsSafeToCall,
        ),
        ProviderTestCase(
            name = getString(LocaleR.string.ptest_method_get_links),
            test = ::methodGetLinksIsSafeToCall,
        ),
    )

    private val module = SerializersModule {
        polymorphic(Film::class) {
            subclass(FilmSearchItem::class, FilmSearchItem.serializer())
        }

        polymorphic(FilmDetails::class) {
            subclass(Movie::class, Movie.serializer())
            subclass(TvShow::class, TvShow.serializer())
        }
    }

    /** For pretty printing */
    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
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
        val (value, timeTaken) = measureTimedValue {
            try {
                val data = assert()
                AssertionResult(
                    status = TestStatus.SUCCESS,
                    data = data,
                )
            } catch (e: Throwable) {
                val status = when (e) {
                    is NotImplementedError -> TestStatus.NOT_IMPLEMENTED
                    else -> TestStatus.FAILURE
                }

                AssertionResult(
                    status = status,
                    error = e,
                )
            }
        }

        val (shortLog, fullLog) = when (value.status) {
            TestStatus.SUCCESS -> successShortLog(value.data) to successFullLog(value.data)
            TestStatus.RUNNING -> UiText.StringValue("") to UiText.StringValue("")
            TestStatus.NOT_IMPLEMENTED, TestStatus.FAILURE -> {
                val actualNotImplementedShortLog = notImplementedShortLog?.invoke(value.error)
                    ?: value.error?.localizedMessage?.let(::createString)
                    ?: getString(LocaleR.string.ptest_error_not_implemented)

                val actualFailedFullLog = failedFullLog?.invoke(value.error)
                    ?: createString(value.error!!.stackTraceToString())

                if (value.status == TestStatus.NOT_IMPLEMENTED)
                    actualNotImplementedShortLog to actualFailedFullLog
                else failedShortLog(value.error) to actualFailedFullLog
            }
        }

        return ProviderTestCaseOutput(
            status = value.status,
            name = testName,
            timeTaken = timeTaken,
            shortLog = shortLog,
            fullLog = fullLog
        )
    }

    private fun List<Film>?.filmsResponseToFullLog(): UiText {
        val films = this?.map {
            "${it.title} [${it.identifier}]"
        }

        return createString(json.encodeToString(films ?: emptyList()))
    }

    private fun getShortLogForItemsFound(size: Int?): UiText {
        if (size == null || size == 0)
            return getString(LocaleR.string.ptest_success_items_count_found_empty)

        return getString(LocaleR.string.ptest_success_items_count_found, size)
    }

    suspend fun testFilmIsSafeToGet(
        testName: UiText,
        api: ProviderApi,
    ): ProviderTestCaseOutput {
        return assertWithMeasuredTime(
            testName = testName,
            failedShortLog = { getString(LocaleR.string.ptest_error_get_testable_film) },
            successShortLog = { createString(api.testFilm.title) },
            successFullLog = { createString(json.encodeToString(api.testFilm)) },
            assert = { assertProperty(api.testFilm) }
        )
    }

    suspend fun catalogListIsSafeToGet(
        testName: UiText,
        api: ProviderApi,
    ): ProviderTestCaseOutput {
        return assertWithMeasuredTime(
            testName = testName,
            failedShortLog = { getString(LocaleR.string.ptest_error_get_catalog_list) },
            successShortLog = { getShortLogForItemsFound(api.catalogs.size) },
            successFullLog = { createString(json.encodeToString(api.catalogs)) },
            assert = { assertProperty(api.catalogs) }
        )
    }

    suspend fun filtersAreSafeToGet(
        testName: UiText,
        api: ProviderApi,
    ): ProviderTestCaseOutput {
        return assertWithMeasuredTime(
            testName = testName,
            failedShortLog = { getString(LocaleR.string.ptest_error_get_search_filters) },
            successShortLog = { getShortLogForItemsFound(api.filters.size) },
            successFullLog = {
                val mapOfFilters = api.filters.withIndex().associate { (index, list) ->
                    val key = list.firstOrNull()?.name ?: "Filter #${index + 1}"
                    val value = list.map {
                        mapOf(
                            "Name" to it.name,
                            "Default value" to it.state.toString()
                        )
                    }

                    key to value
                }

                createString(json.encodeToString(mapOfFilters))
            },
            assert = {
                api.filters.forEach { filterGroup ->
                    filterGroup.forEach { filter ->
                        assertProperty(filter)
                    }
                }
            }
        )
    }

    suspend fun methodGetCatalogItemsIsSafeToCall(
        testName: UiText,
        api: ProviderApi,
    ): ProviderTestCaseOutput {
        return assertWithMeasuredTime(
            testName = testName,
            failedShortLog = {
                val catalogTested = if (api.catalogs.isEmpty()) {
                    ""
                } else "- ${api.catalogs.first().name}"

                getString(
                    LocaleR.string.ptest_error_method_get_catalog_items,
                    catalogTested
                )
            },
            successShortLog = { getShortLogForItemsFound(it?.size) },
            successFullLog = { it.filmsResponseToFullLog() },
            assert = {
                if (api.catalogs.isEmpty())
                    throw NotImplementedError("Provider catalogs are empty")

                val response = api.getCatalogItems(
                    catalog = api.catalogs.first(),
                )

                response.results
            }
        )
    }

    suspend fun methodSearchIsSafeToCall(
        testName: UiText,
        api: ProviderApi,
    ): ProviderTestCaseOutput {
        return assertWithMeasuredTime(
            testName = testName,
            failedShortLog = {
                val catalogTested = if (api.catalogs.isEmpty()) {
                    ""
                } else "- ${api.catalogs.first().name}"

                getString(
                    LocaleR.string.ptest_error_method_get_catalog_items,
                    catalogTested
                )
            },
            successShortLog = { getShortLogForItemsFound(it?.size) },
            successFullLog = { it.filmsResponseToFullLog() },
            assert = {
                val response = with(api.testFilm) {
                    api.search(
                        title = title,
                        id = id,
                        imdbId = imdbId,
                        tmdbId = tmdbId,
                    )
                }

                response.results
            }
        )
    }

    suspend fun methodGetFilmDetailsIsSafeToCall(
        testName: UiText,
        api: ProviderApi,
    ): ProviderTestCaseOutput {
        return assertWithMeasuredTime(
            testName = testName,
            failedShortLog = {
                getString(
                    LocaleR.string.ptest_error_method_get_film_details,
                    "${api.testFilm.title} [${api.testFilm.identifier}]"
                )
            },
            successShortLog = { createString(it?.title ?: api.testFilm.title) },
            successFullLog = { createString(json.encodeToString(it)) },
            assert = { api.getFilmDetails(api.testFilm) }
        )
    }

    suspend fun methodGetLinksIsSafeToCall(
        testName: UiText,
        api: ProviderApi,
    ): ProviderTestCaseOutput {
        return assertWithMeasuredTime(
            testName = testName,
            failedShortLog = {
                getString(
                    LocaleR.string.ptest_error_method_get_links,
                    "${api.testFilm.title} [${api.testFilm.identifier}]"
                )
            },
            successShortLog = { getShortLogForItemsFound(it?.size) },
            successFullLog = { links ->
                val linksOnly = links?.map { it.url }

                createString(json.encodeToString(linksOnly))
            },
            assert = {
                val episodeData = when (api.testFilm.filmType) {
                    FilmType.TV_SHOW -> Episode(number = 1, season = 1)
                    else -> null
                }

                val links = mutableSetOf<MediaLink>()
                if (api is ProviderWebViewApi) {
                    val webView = withMainContext {
                        api.getWebView()
                    }

                    webView.getLinks(
                        watchId = api.testFilm.id ?: api.testFilm.identifier,
                        film = api.testFilm,
                        episode = episodeData,
                        onLinkFound = links::add
                    )
                } else {
                    api.getLinks(
                        watchId = api.testFilm.id ?: api.testFilm.identifier,
                        film = api.testFilm,
                        episode = episodeData,
                        onLinkFound = links::add
                    )
                }

                return@assertWithMeasuredTime links
            }
        )
    }
}