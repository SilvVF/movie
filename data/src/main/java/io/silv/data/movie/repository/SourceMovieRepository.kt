package io.silv.data.movie.repository

import androidx.paging.PagingSource
import io.silv.core.SGenre
import io.silv.core.SMovie
import io.silv.core.STVShow
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

typealias MoviePagingSourceType = PagingSource<Long, SMovie>
typealias TVPagingSourceType = PagingSource<Long, STVShow>

interface SourceMovieRepository {

    suspend fun getSourceGenres(): List<SGenre>

    fun discoverMovies(genres: List<String>): SourcePagingSource

    fun searchMovies(query: String): MoviePagingSourceType

    fun getNowPlayingMovies(): MoviePagingSourceType

    fun getPopularMovies(): MoviePagingSourceType

    fun getUpcomingMovies(): MoviePagingSourceType

    fun getTopRatedMovies(): MoviePagingSourceType
}

interface SourceTVRepository {

    suspend fun getSourceGenres(): List<SGenre>

    fun discover(genres: List<String>): TVPagingSourceType

    fun search(query: String): TVPagingSourceType

    fun nowPlaying(): TVPagingSourceType

    fun popular(): TVPagingSourceType

    fun upcoming(): TVPagingSourceType

    fun topRated(): TVPagingSourceType
}

class SourceTVRepositoryImpl(
    private val tvService: TMDBTVShowService
): SourceTVRepository {

    override suspend fun getSourceGenres(): List<SGenre> {
        return TMDBConstants.genres
    }

    override fun discover(genres: List<String>): TVPagingSourceType {
        return DiscoverTVPagingSource(genres, tvService)
    }

    override fun search(query: String): TVPagingSourceType {
        return SearchTVPagingSource(query, tvService)
    }

    override fun nowPlaying(): TVPagingSourceType {
        return NowPlayingTVPagingSource(tvService)
    }
    override fun popular(): TVPagingSourceType {
        return PopularTVPagingSource(tvService)
    }
    override fun upcoming(): TVPagingSourceType {
        return UpcomingTVPagingSource(tvService)
    }

    override fun topRated(): TVPagingSourceType {
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

    override fun searchMovies(query: String): MoviePagingSourceType {
        return SearchMoviePagingSource(query, movieService)
    }

    override fun getNowPlayingMovies(): MoviePagingSourceType {
        return NowPlayingMoviePagingSource(movieService)
    }
    override fun getPopularMovies(): MoviePagingSourceType {
        return PopularMoviePagingSource(movieService)
    }
    override fun getUpcomingMovies(): MoviePagingSourceType {
        return UpcomingMoviePagingSource(movieService)
    }

    override fun getTopRatedMovies(): MoviePagingSourceType {
        return TopRatedMoviePagingSource(movieService)
    }
}