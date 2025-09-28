package com.flixclusive.domain.catalog.usecase.impl

import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.data.tmdb.model.TMDBDiscoverCatalog
import com.flixclusive.data.tmdb.repository.TMDBDiscoverCatalogRepository
import com.flixclusive.data.tmdb.repository.TMDBFilmSearchItemsRepository
import com.flixclusive.domain.catalog.model.DiscoverCards
import com.flixclusive.domain.catalog.usecase.GetDiscoverCardsUseCase
import javax.inject.Inject

internal class GetDiscoverCardsUseCaseImpl
    @Inject
    constructor(
        private val tmdbDiscoverCatalogRepository: TMDBDiscoverCatalogRepository,
        private val tmdbFilmSearchItemsRepository: TMDBFilmSearchItemsRepository,
    ) : GetDiscoverCardsUseCase {
        private var cards: DiscoverCards? = null
        private val usedThumbnails = mutableSetOf<String>()

        override suspend operator fun invoke(): Resource<DiscoverCards> {
            return try {
                if (cards != null) {
                    return Resource.Success(cards!!)
                }

                usedThumbnails.clear()

                val tvNetworks = getTvNetworks()
                val movieCompanies = getMovieCompanies()
                val categories = getCategories()

                Resource.Success(
                    DiscoverCards(
                        categories = categories,
                        tvNetworks = tvNetworks,
                        movieCompanies = movieCompanies,
                    ).also { cards = it },
                )
            } catch (e: Exception) {
                errorLog(e)
                Resource.Failure(e)
            }
        }

        /**
         * Fetches TV networks from the repository and assigns a unique thumbnail image to each network.
         * */
        private suspend fun getTvNetworks(): List<TMDBDiscoverCatalog> {
            return tmdbDiscoverCatalogRepository
                .getTvNetworks()
                .sortedBy {
                    it.name
                }.map {
                    it.copy(image = getThumbnail(it))
                }
        }

        /**
         * Fetches movie production companies from the repository and assigns a unique thumbnail image to each company.
         * */
        private suspend fun getMovieCompanies(): List<TMDBDiscoverCatalog> {
            return tmdbDiscoverCatalogRepository
                .getMovieCompanies()
                .sortedBy {
                    it.name
                }.map {
                    it.copy(image = getThumbnail(it))
                }
        }

        /**
         * Fetches movie and TV genres and assigns a unique thumbnail image to each genre.
         * */
        private suspend fun getCategories(): List<TMDBDiscoverCatalog> {
            val mediaTypes = tmdbDiscoverCatalogRepository.getTv() + tmdbDiscoverCatalogRepository.getMovies()
            val genres = tmdbDiscoverCatalogRepository.getGenres()

            val categories = mediaTypes + genres

            return categories.map {
                it.copy(image = getThumbnail(it))
            }
        }

        /**
         * Tries to get a thumbnail for the given catalog by fetching items from the associated URL.
         *
         * It will attempt to find a non-null backdrop image from random items across multiple pages,
         * ensuring that the same thumbnail is not reused for different catalogs. If no suitable thumbnail
         * is found after checking up to 5 pages, it returns null.
         *
         * @param catalog The [TMDBDiscoverCatalog] for which to find a thumbnail.
         *
         * @return A [String] URL of the thumbnail image, or null if none could be found.
         * */
        private suspend fun getThumbnail(catalog: TMDBDiscoverCatalog): String? {
            var page = 1
            var thumbnail: String? = null
            while (thumbnail == null && page <= 3) {
                val response = tmdbFilmSearchItemsRepository.get(
                    url = catalog.url,
                    page = page,
                )

                val data = response.data
                if (data == null || response is Resource.Failure) {
                    page++
                    continue
                }

                val items = data.results
                // Be very careful of infinite loop
                while (thumbnail == null && items.isNotEmpty()) {
                    val item = items.random()
                    thumbnail = item.backdropImage
                }

                if (thumbnail != null) {
                    usedThumbnails.add(thumbnail)
                    return thumbnail
                }

                page++
            }

            return null
        }
    }
