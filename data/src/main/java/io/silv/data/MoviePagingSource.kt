package io.silv.data

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import io.silv.core.SMovie
import io.silv.core.await
import io.silv.core_network.TMDBMovieService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class MoviesPage(val movies: List<SMovie>, val hasNextPage: Boolean)

class SearchMovePagingSource(
    private val query: String,
    private val movieService: TMDBMovieService
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


class NowPlayingMovePagingSource(
    private val movieService: TMDBMovieService
): SourcePagingSource() {

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


class TopRatedMovePagingSource(
    private val movieService: TMDBMovieService
): SourcePagingSource() {

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

class UpcomingMovePagingSource(
    private val movieService: TMDBMovieService
): SourcePagingSource() {

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

class PopularMovePagingSource(
    private val movieService: TMDBMovieService
): SourcePagingSource() {

    override suspend fun getNextPage(page: Int): MoviesPage {

        val response = movieService.movieList(
            type = TMDBMovieService.MovieType.Popular.toString(),
            page = page
        )
            .await()
            .also {
                Log.d("RESPONSE", it.errorBody()?.string() ?: "")
            }
            .body()!!

        return MoviesPage(
            movies = response.results.map { it.toSMovie() },
            hasNextPage = response.page < response.totalPages
        )
    }
}

abstract class SourcePagingSource : PagingSource<Long, SMovie>() {


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
            Log.d("MoviePagingSource", e.stackTraceToString())
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