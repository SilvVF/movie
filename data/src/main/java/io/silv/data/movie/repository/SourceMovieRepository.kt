package io.silv.data.movie.repository

import androidx.paging.PagingSource
import io.silv.core.SGenre
import io.silv.core.SMovie
import io.silv.core_network.TMDBConstants
import io.silv.core_network.TMDBMovieService
import io.silv.core_network.TMDBTVShowService
import io.silv.data.movie.interactor.DiscoverMoviesPagingSource
import io.silv.data.movie.interactor.DiscoverTVPagingSource
import io.silv.data.movie.interactor.NowPlayingMoviePagingSource
import io.silv.data.movie.interactor.NowPlayingTVPagingSource
import io.silv.data.movie.interactor.PopularMoviePagingSource
import io.silv.data.movie.interactor.PopularTVPagingSource
import io.silv.data.movie.interactor.SearchMoviePagingSource
import io.silv.data.movie.interactor.SearchTVPagingSource
import io.silv.data.movie.interactor.SourcePagingSource
import io.silv.data.movie.interactor.TopRatedMoviePagingSource
import io.silv.data.movie.interactor.TopRatedTVPagingSource
import io.silv.data.movie.interactor.UpcomingMoviePagingSource
import io.silv.data.movie.interactor.UpcomingTVPagingSource

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

interface SourceTVRepository {

    suspend fun getSourceGenres(): List<SGenre>

    fun discoverMovies(genres: List<String>): SourcePagingSource

    fun searchMovies(query: String): SourcePagingSourceType

    fun getNowPlayingMovies(): SourcePagingSourceType

    fun getPopularMovies(): SourcePagingSourceType

    fun getUpcomingMovies(): SourcePagingSourceType

    fun getTopRatedMovies(): SourcePagingSourceType
}

class SourceTVRepositoryImpl(
    private val tvService: TMDBTVShowService
): SourceTVRepository {

    override suspend fun getSourceGenres(): List<SGenre> {
        return TMDBConstants.genres
    }

    override fun discoverMovies(genres: List<String>): SourcePagingSource {
        return DiscoverTVPagingSource(genres, tvService)
    }

    override fun searchMovies(query: String): SourcePagingSourceType {
        return SearchTVPagingSource(query, tvService)
    }

    override fun getNowPlayingMovies(): SourcePagingSourceType {
        return NowPlayingTVPagingSource(tvService)
    }
    override fun getPopularMovies(): SourcePagingSourceType {
        return PopularTVPagingSource(tvService)
    }
    override fun getUpcomingMovies(): SourcePagingSourceType {
        return UpcomingTVPagingSource(tvService)
    }

    override fun getTopRatedMovies(): SourcePagingSourceType {
        return TopRatedTVPagingSource(tvService)
    }
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
        return SearchMoviePagingSource(query, movieService)
    }

    override fun getNowPlayingMovies(): SourcePagingSourceType {
        return NowPlayingMoviePagingSource(movieService)
    }
    override fun getPopularMovies(): SourcePagingSourceType {
        return PopularMoviePagingSource(movieService)
    }
    override fun getUpcomingMovies(): SourcePagingSourceType {
        return UpcomingMoviePagingSource(movieService)
    }

    override fun getTopRatedMovies(): SourcePagingSourceType {
        return TopRatedMoviePagingSource(movieService)
    }
}