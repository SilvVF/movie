package io.silv.data.movie.interactor

import io.silv.data.movie.model.MoviePagedType
import io.silv.data.movie.repository.SourceMovieRepository
import io.silv.data.movie.repository.SourcePagingSourceType

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