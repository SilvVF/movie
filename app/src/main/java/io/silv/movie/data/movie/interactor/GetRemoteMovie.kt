package io.silv.movie.data.movie.interactor

import io.silv.movie.data.movie.model.ContentPagedType
import io.silv.movie.data.movie.repository.MoviePagingSourceType
import io.silv.movie.data.movie.repository.SourceMovieRepository
import io.silv.movie.data.movie.repository.SourceTVRepository
import io.silv.movie.data.movie.repository.TVPagingSourceType

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
            is ContentPagedType.Discover -> sourceMovieRepository.discoverMovies(type.genres)
        }
    }
}

class GetRemoteTVShows(
    private val tvRepository: SourceTVRepository
) {

    fun subscribe(type: ContentPagedType): TVPagingSourceType {
        return when (type) {
            is ContentPagedType.Search -> tvRepository.search(type.query)
            is ContentPagedType.Default -> {
                when(type) {
                    ContentPagedType.Default.Popular -> tvRepository.popular()
                    ContentPagedType.Default.TopRated -> tvRepository.topRated()
                    ContentPagedType.Default.Upcoming -> tvRepository.upcoming()
                }
            }
            is ContentPagedType.Discover -> tvRepository.discover(type.genres)
        }
    }
}