package io.silv.movie.data.lists

import io.silv.movie.data.movie.repository.MovieRepository
import io.silv.movie.data.tv.repository.ShowRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GetFavoritesList(
    private val movieRepository: MovieRepository,
    private val showRepository: ShowRepository
) {

    fun subscribe(query: String = ""): Flow<List<ContentItem>> {
        return combine(
            movieRepository.observeFavorites(query),
            showRepository.observeFavorites(query)
        ) { moviePosters, tvShowPosters ->
            moviePosters.map {
                ContentItem(
                    it.id,
                    isMovie = true,
                    title = it.title,
                    posterUrl = it.posterUrl,
                    it.posterLastUpdated,
                    it.favorite,
                    it.lastModifiedAt,
                    it.overview,
                    it.popularity
                )
            } + tvShowPosters.map {
                ContentItem(
                    it.id,
                    isMovie = false,
                    title = it.title,
                    posterUrl = it.posterUrl,
                    it.posterLastUpdated,
                    it.favorite,
                    it.lastModifiedAt,
                    it.overview,
                    it.popularity
                )
            }
        }
    }
}