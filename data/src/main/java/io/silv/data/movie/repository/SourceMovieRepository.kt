package io.silv.data.movie.repository

import androidx.paging.PagingSource
import io.silv.core.SGenre
import io.silv.core.SMovie
import io.silv.core_network.TMDBConstants
import io.silv.core_network.TMDBMovieService
import io.silv.data.movie.interactor.DiscoverMoviesPagingSource
import io.silv.data.movie.interactor.NowPlayingMovePagingSource
import io.silv.data.movie.interactor.PopularMovePagingSource
import io.silv.data.movie.interactor.SearchMovePagingSource
import io.silv.data.movie.interactor.SourcePagingSource
import io.silv.data.movie.interactor.TopRatedMovePagingSource
import io.silv.data.movie.interactor.UpcomingMovePagingSource

typealias SourcePagingSourceType = PagingSource<Long, SMovie>

interface SourceMovieRepository {

    suspend fun getSourceGenres(): List<SGenre>

    fun discoverMovies(genres: List<String>): SourcePagingSource

    fun searchMovies(query: String): SourcePagingSourceType

    fun getNowPlayingMovies(): SourcePagingSourceType

    fun getPopularMovies(): SourcePagingSourceType

    fun getUpcomingMovies(): SourcePagingSourceType

    fun getTopRatedMovies(): SourcePagingSourceType
}

class SourceMovieRepositoryImpl(
    private val movieService: TMDBMovieService
): SourceMovieRepository {

    override suspend fun getSourceGenres(): List<SGenre> {
        return TMDBConstants.genres
    }

    override fun discoverMovies(genres: List<String>): SourcePagingSource {
        return DiscoverMoviesPagingSource(genres, movieService)
    }

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