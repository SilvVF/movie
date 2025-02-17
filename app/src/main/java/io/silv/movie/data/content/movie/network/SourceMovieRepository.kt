package io.silv.movie.data.content.movie.network

import androidx.paging.PagingSource
import io.silv.movie.core.SGenre
import io.silv.movie.core.SMovie
import io.silv.movie.core.await
import io.silv.movie.data.content.movie.model.ContentPagedType
import io.silv.movie.data.content.movie.model.Filters
import io.silv.movie.network.service.tmdb.TMDBConstants
import io.silv.movie.network.service.tmdb.TMDBMovieService

typealias MoviePagingSourceType = PagingSource<Long, SMovie>

interface SourceMovieRepository {

    suspend fun getMovie(id: Long): SMovie?

    suspend fun getMovieGenres(): List<SGenre>

    fun discoverMovies(filters: Filters): SourceMoviePagingSource

    fun searchMovies(query: String): MoviePagingSourceType

    fun getNowPlayingMovies(): MoviePagingSourceType

    fun getPopularMovies(): MoviePagingSourceType

    fun getUpcomingMovies(): MoviePagingSourceType

    fun getTopRatedMovies(): MoviePagingSourceType
}

fun SourceMovieRepository.getMoviePager(type: ContentPagedType) = when (type) {
    is ContentPagedType.Search -> searchMovies(type.query)
    is ContentPagedType.Default -> {
        when (type) {
            ContentPagedType.Default.Popular -> getPopularMovies()
            ContentPagedType.Default.TopRated -> getTopRatedMovies()
            ContentPagedType.Default.Upcoming -> getUpcomingMovies()
        }
    }
    is ContentPagedType.Discover -> discoverMovies(type.filters)
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
            posterPath = "https://image.tmdb.org/t/p/original${details.posterPath}".takeIf { details.posterPath.orEmpty().isNotBlank() }
            adult = details.adult
            originalTitle = details.originalTitle
            voteAverage = details.voteAverage
            productionCompanies = details.productionCompanies.map { it.name }
            status = io.silv.movie.core.Status.fromString(details.status)
        }
    }

    override suspend fun getMovieGenres(): List<SGenre> {
        return TMDBConstants.genres
    }

    override fun discoverMovies(filters: Filters): SourceMoviePagingSource {
        return DiscoverMoviesPagingSource(filters, movieService)
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