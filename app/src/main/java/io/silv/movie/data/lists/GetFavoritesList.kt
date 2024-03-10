package io.silv.movie.data.lists

import io.silv.core_ui.components.PosterData
import io.silv.movie.data.movie.repository.MovieRepository
import io.silv.movie.data.tv.repository.ShowRepository
import io.silv.movie.presentation.toPoster
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GetFavoritesList(
    private val movieRepository: MovieRepository,
    private val showRepository: ShowRepository
) {

    fun subscribe(): Flow<List<PosterData>> {
        return combine(
            movieRepository.observeFavorites(),
            showRepository.observeFavorites()
        ) { moviePosters, tvShowPosters ->
            moviePosters.map { it.toPoster() } + tvShowPosters.map { it.toPoster() }
        }
    }
}