package io.silv.data.movie.interactor

import io.silv.data.movie.model.MoviePagedType
import io.silv.data.movie.repository.SourceMovieRepository
import io.silv.data.movie.repository.SourcePagingSourceType
import io.silv.data.movie.repository.SourceTVRepository

class GetRemoteMovie(
    private val sourceMovieRepository: SourceMovieRepository
) {

    fun subscribe(type: MoviePagedType): SourcePagingSourceType {
        return when (type) {
            is MoviePagedType.Search -> sourceMovieRepository.searchMovies(type.query)
            is MoviePagedType.Default -> {
                when(type) {
                    MoviePagedType.Default.Popular -> sourceMovieRepository.getPopularMovies()
                    MoviePagedType.Default.TopRated -> sourceMovieRepository.getTopRatedMovies()
                    MoviePagedType.Default.Upcoming -> sourceMovieRepository.getUpcomingMovies()
                }
            }
            is MoviePagedType.Discover -> sourceMovieRepository.discoverMovies(type.genres)
        }
    }
}

class GetRemoteTVShows(
    private val tvRepository: SourceTVRepository
) {

    fun subscribe(type: MoviePagedType): SourcePagingSourceType {
        return when (type) {
            is MoviePagedType.Search -> tvRepository.searchMovies(type.query)
            is MoviePagedType.Default -> {
                when(type) {
                    MoviePagedType.Default.Popular -> tvRepository.getPopularMovies()
                    MoviePagedType.Default.TopRated -> tvRepository.getTopRatedMovies()
                    MoviePagedType.Default.Upcoming -> tvRepository.getUpcomingMovies()
                }
            }
            is MoviePagedType.Discover -> tvRepository.discoverMovies(type.genres)
        }
    }
}