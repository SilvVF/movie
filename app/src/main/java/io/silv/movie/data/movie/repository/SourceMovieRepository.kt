package io.silv.movie.data.movie.repository

import androidx.paging.PagingSource
import io.silv.movie.core.SGenre
import io.silv.movie.core.SMovie
import io.silv.movie.core.STVShow
import io.silv.movie.core.await
import io.silv.movie.data.movie.interactor.DiscoverMoviesPagingSource
import io.silv.movie.data.movie.interactor.NowPlayingMoviePagingSource
import io.silv.movie.data.movie.interactor.PopularMoviePagingSource
import io.silv.movie.data.movie.interactor.SearchMoviePagingSource
import io.silv.movie.data.movie.interactor.SourceMoviePagingSource
import io.silv.movie.data.movie.interactor.TopRatedMoviePagingSource
import io.silv.movie.data.movie.interactor.UpcomingMoviePagingSource
import io.silv.movie.data.tv.interactor.DiscoverTVPagingSource
import io.silv.movie.data.tv.interactor.NowPlayingTVPagingSource
import io.silv.movie.data.tv.interactor.PopularTVPagingSource
import io.silv.movie.data.tv.interactor.SearchTVPagingSource
import io.silv.movie.data.tv.interactor.TopRatedTVPagingSource
import io.silv.movie.data.tv.interactor.UpcomingTVPagingSource
import io.silv.movie.network.service.tmdb.TMDBConstants
import io.silv.movie.network.service.tmdb.TMDBMovieService
import io.silv.movie.network.service.tmdb.TMDBTVShowService

typealias MoviePagingSourceType = PagingSource<Long, SMovie>
typealias TVPagingSourceType = PagingSource<Long, STVShow>

interface SourceMovieRepository {

    suspend fun getMovie(id: Long): SMovie?

    suspend fun getSourceGenres(): List<SGenre>

    fun discoverMovies(genres: List<String>): SourceMoviePagingSource

    fun searchMovies(query: String): MoviePagingSourceType

    fun getNowPlayingMovies(): MoviePagingSourceType

    fun getPopularMovies(): MoviePagingSourceType

    fun getUpcomingMovies(): MoviePagingSourceType

    fun getTopRatedMovies(): MoviePagingSourceType
}

interface SourceTVRepository {

    suspend fun getShow(id: Long): STVShow?

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
    override suspend fun getShow(id: Long): STVShow? {
        val details = tvService.details(id).await().body() ?: return null
        return STVShow.create().apply {
            this.id = id
            url = "https://api.themoviedb.org/3/tv/$id"
            posterPath = "https://image.tmdb.org/t/p/original${details.posterPath}".takeIf { details.posterPath.isNotBlank() }
            adult = details.adult
            releaseDate = details.firstAirDate
            overview = details.overview
            genres = details.genres.map { Pair(it.id, it.name) }
            genreIds = details.genres.map { it.id }
            originalLanguage = details.originalLanguage
            originalTitle = details.originalName
            title = details.name
            backdropPath = details.backdropPath
            popularity = details.popularity
            voteCount = details.voteCount
            voteAverage = details.voteAverage
            productionCompanies = details.productionCompanies.map { it.name }
            status = io.silv.movie.core.Status.fromString(details.status)
        }
    }

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

    override suspend fun getMovie(id: Long): SMovie? {
        val details = movieService.details(id).await().body() ?: return null
        return SMovie.create().apply {
            this.id = id
            title = details.title
            overview = details.overview
            genres = details.genres.map { Pair(it.id, it.name) }
            genreIds = details.genres.map { it.id }
            originalLanguage = details.originalLanguage
            popularity = details.popularity
            voteCount = details.voteCount
            releaseDate = details.releaseDate
            url = "https://api.themoviedb.org/3/movie/$id"
            posterPath = "https://image.tmdb.org/t/p/original${details.posterPath}".takeIf { details.posterPath.isNotBlank() }
            adult = details.adult
            originalTitle = details.originalTitle
            voteAverage = details.voteAverage
            productionCompanies = details.productionCompanies.map { it.name }
            status = io.silv.movie.core.Status.fromString(details.status)
        }
    }

    override suspend fun getSourceGenres(): List<SGenre> {
        return TMDBConstants.genres
    }

    override fun discoverMovies(genres: List<String>): SourceMoviePagingSource {
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