package io.silv.data

import androidx.paging.PagingSource
import io.silv.core.SMovie
import io.silv.core_network.TMDBMovieService

typealias SourcePagingSourceType = PagingSource<Long, SMovie>

interface SourceMovieRepository {

    fun searchMovies(query: String): SourcePagingSourceType

    fun getNowPlayingMovies(): SourcePagingSourceType

    fun getPopularMovies(): SourcePagingSourceType

    fun getUpcomingMovies(): SourcePagingSourceType

    fun getTopRatedMovies(): SourcePagingSourceType
}

class SourceMovieRepositoryImpl(
    private val movieService: TMDBMovieService
): SourceMovieRepository {

    override fun searchMovies(query: String): SourcePagingSourceType {
        return SearchMovePagingSource(query, movieService)
    }

    override fun getNowPlayingMovies(): SourcePagingSourceType {
        return NowPlayingMovePagingSource(movieService)
    }
    override fun getPopularMovies(): SourcePagingSourceType {
        return PopularMovePagingSource(movieService)
    }
    override fun getUpcomingMovies(): SourcePagingSourceType {
        return UpcomingMovePagingSource(movieService)
    }

    override fun getTopRatedMovies(): SourcePagingSourceType {
        return TopRatedMovePagingSource(movieService)
    }
}