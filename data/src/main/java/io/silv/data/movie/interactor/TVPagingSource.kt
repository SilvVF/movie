package io.silv.data.movie.interactor

import io.silv.core.await
import io.silv.core_network.TMDBConstants
import io.silv.core_network.TMDBTVShowService
import io.silv.core_network.model.toSMovie


class DiscoverTVPagingSource(
    private val genres: List<String>,
    private val movieService: TMDBTVShowService
): SourcePagingSource() {
    override suspend fun getNextPage(page: Int): MoviesPage {
        val response = movieService.discover(
            page = page,
            genres = TMDBConstants.genresString(genres, TMDBConstants.JOIN_MODE_MASK_OR)
        )
            .await()
            .body()!!

        return MoviesPage(
            movies = response.results.map { it.toSMovie() },
            hasNextPage = response.page < response.totalPages
        )
    }
}

class SearchTVPagingSource(
    private val query: String,
    private val movieService: TMDBTVShowService
): SourcePagingSource() {

    override suspend fun getNextPage(page: Int): MoviesPage {
        val response = movieService.search(
            query = query,
            page = page
        )
            .await()
            .body()!!

        return MoviesPage(
            movies = response.results.map { it.toSMovie() },
            hasNextPage = response.page < response.totalPages
        )
    }
}


class NowPlayingTVPagingSource(
    private val movieService: TMDBTVShowService
): SourcePagingSource() {

    override suspend fun getNextPage(page: Int): MoviesPage {
        val response = movieService.tvList(
            type = TMDBTVShowService.TVType.NowPlaying.toString(),
            page = page
        )
            .await()
            .body()!!

        return MoviesPage(
            movies = response.results.map { it.toSMovie() },
            hasNextPage = response.page < response.totalPages
        )
    }
}


class TopRatedTVPagingSource(
    private val movieService: TMDBTVShowService
): SourcePagingSource() {

    override suspend fun getNextPage(page: Int): MoviesPage {
        val response = movieService.tvList(
            type = TMDBTVShowService.TVType.TopRated.toString(),
            page = page
        )
            .await()
            .body()!!

        return MoviesPage(
            movies = response.results.map { it.toSMovie() },
            hasNextPage = response.page < response.totalPages
        )
    }
}

class UpcomingTVPagingSource(
    private val movieService: TMDBTVShowService
): SourcePagingSource() {

    override suspend fun getNextPage(page: Int): MoviesPage {
        val response = movieService.tvList(
            type = TMDBTVShowService.TVType.Upcoming.toString(),
            page = page
        )
            .await()
            .body()!!

        return MoviesPage(
            movies = response.results.map { it.toSMovie() },
            hasNextPage = response.page < response.totalPages
        )
    }
}

class PopularTVPagingSource(
    private val movieService: TMDBTVShowService
): SourcePagingSource() {

    override suspend fun getNextPage(page: Int): MoviesPage {

        val response = movieService.tvList(
            type = TMDBTVShowService.TVType.Popular.toString(),
            page = page
        )
            .await()
            .body()!!

        return MoviesPage(
            movies = response.results.map { it.toSMovie() },
            hasNextPage = response.page < response.totalPages
        )
    }
}