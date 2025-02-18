package io.silv.movie.data.network

import androidx.paging.PagingSource
import androidx.paging.PagingState
import io.silv.movie.core.SMovie
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.silv.movie.core.await
import io.silv.movie.data.model.Filters
import io.silv.movie.data.model.GenreMode
import io.silv.movie.api.model.toSMovie
import io.silv.movie.api.service.tmdb.TMDBConstants
import io.silv.movie.api.service.tmdb.TMDBConstants.JOIN_MODE_MASK_AND
import io.silv.movie.api.service.tmdb.TMDBConstants.JOIN_MODE_MASK_OR
import io.silv.movie.api.service.tmdb.TMDBMovieService


data class MoviesPage(val movies: List<SMovie>, val hasNextPage: Boolean)

class DiscoverMoviesPagingSource(
    private val filters: Filters,
    private val movieService: TMDBMovieService
): SourceMoviePagingSource() {
    override suspend fun getNextPage(page: Int): MoviesPage {
        val response = movieService.discover(
            page = page,
            genres = TMDBConstants.genresString(
                filters.genres.map { it.name },
                if(filters.genreMode == GenreMode.Or) JOIN_MODE_MASK_OR else JOIN_MODE_MASK_AND
            ),
            sortBy = filters.sortingOption.sort,
            companies = filters.companies.value.ifBlank { null },
            people = filters.people.value.ifBlank { null },
            keywords = filters.keywords.value.ifBlank { null },
            year = filters.year.value.toIntOrNull(),
            voteAverage = filters.voteAverage.value.toFloatOrNull(),
            voteCount = filters.voteCount.value.toFloatOrNull()
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


abstract class SourceMoviePagingSource : PagingSource<Long, SMovie>() {

    abstract suspend fun getNextPage(page: Int): MoviesPage

    override suspend fun load(params: LoadParams<Long>): LoadResult<Long, SMovie> {
        val page = params.key ?: 1

        val moviesPage = try {
            withContext(Dispatchers.IO) {
                getNextPage(page.toInt())
                    .takeIf { it.movies.isNotEmpty() }
                    ?: error("Empty page")
            }
        } catch (e: Exception) {
            return LoadResult.Error(e)
        }

        return LoadResult.Page(
            data = moviesPage.movies,
            prevKey = null,
            nextKey = if (moviesPage.hasNextPage) page + 1 else null,
        )
    }

    override fun getRefreshKey(state: PagingState<Long, SMovie>): Long? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey ?: anchorPage?.nextKey
        }
    }
}