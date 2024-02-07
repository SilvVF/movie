package io.silv.data

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
        }
    }
}