package io.silv.movie.data.movie.interactor

import io.silv.movie.core.SMovie
import io.silv.movie.core.await
import io.silv.movie.network.model.toSMovie
import io.silv.movie.network.service.tmdb.TMDBConstants
import io.silv.movie.network.service.tmdb.TMDBMovieService

data class MoviesPage(val movies: List<SMovie>, val hasNextPage: Boolean)

class DiscoverMoviesPagingSource(
    private val genres: List<String>,
    private val movieService: TMDBMovieService
): SourceMoviePagingSource() {
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

class SearchMoviePagingSource(
    private val query: String,
    private val movieService: TMDBMovieService
): SourceMoviePagingSource() {

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


class NowPlayingMoviePagingSource(
    private val movieService: TMDBMovieService
): SourceMoviePagingSource() {

    override suspend fun getNextPage(page: Int): MoviesPage {
        val response = movieService.movieList(
            type = TMDBMovieService.MovieType.NowPlaying.toString(),
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


class TopRatedMoviePagingSource(
    private val movieService: TMDBMovieService
): SourceMoviePagingSource() {

    override suspend fun getNextPage(page: Int): MoviesPage {
        val response = movieService.movieList(
            type = TMDBMovieService.MovieType.TopRated.toString(),
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

class UpcomingMoviePagingSource(
    private val movieService: TMDBMovieService
): SourceMoviePagingSource() {

    override suspend fun getNextPage(page: Int): MoviesPage {
        val response = movieService.movieList(
            type = TMDBMovieService.MovieType.Upcoming.toString(),
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

class PopularMoviePagingSource(
    private val movieService: TMDBMovieService
): SourceMoviePagingSource() {

    override suspend fun getNextPage(page: Int): MoviesPage {

        val response = movieService.movieList(
            type = TMDBMovieService.MovieType.Popular.toString(),
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
