package io.silv.movie.data.content.lists.interactor

import io.silv.movie.data.content.lists.ContentItem
import io.silv.movie.data.content.lists.repository.ContentListRepository
import io.silv.movie.presentation.screenmodel.FavoritesSortMode
import kotlinx.coroutines.flow.Flow

class GetFavoritesList(
    private val contentListRepository: ContentListRepository
) {

    fun subscribe(query: String = "", sortMode: FavoritesSortMode = FavoritesSortMode.Title): Flow<List<ContentItem>> {
        return contentListRepository.observeFavorites(query, sortMode)
    }
}