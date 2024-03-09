package io.silv.movie.data.movie.interactor

import io.silv.movie.data.ContentPagedType
import io.silv.movie.data.movie.repository.MoviePagingSourceType
import io.silv.movie.data.movie.repository.SourceMovieRepository

class GetRemoteMovie(
    private val sourceMovieRepository: SourceMovieRepository
) {
    suspend fun awaitOne(id: Long): io.silv.movie.core.SMovie? {
        return sourceMovieRepository.getMovie(id)
    }

    fun subscribe(type: ContentPagedType): MoviePagingSourceType {
        return when (type) {
            is ContentPagedType.Search -> sourceMovieRepository.searchMovies(type.query)
            is ContentPagedType.Default -> {
                when(type) {
                    ContentPagedType.Default.Popular -> sourceMovieRepository.getPopularMovies()
                    ContentPagedType.Default.TopRated -> sourceMovieRepository.getTopRatedMovies()
                    ContentPagedType.Default.Upcoming -> sourceMovieRepository.getUpcomingMovies()
                }
            }
            is ContentPagedType.Discover -> sourceMovieRepository.discoverMovies(type.filters)
        }
    }
}
