package io.silv.movie.data.lists

import io.silv.movie.presentation.library.view.favorite.FavoritesSortMode
import kotlinx.coroutines.flow.Flow

class GetFavoritesList(
    private val contentListRepository: ContentListRepository
) {

    fun subscribe(query: String = "", sortMode: FavoritesSortMode = FavoritesSortMode.Title): Flow<List<ContentItem>> {
        return contentListRepository.observeFavorites(query, sortMode)
    }
}