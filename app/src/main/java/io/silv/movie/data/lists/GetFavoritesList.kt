package io.silv.movie.data.lists

import kotlinx.coroutines.flow.Flow

class GetFavoritesList(
    private val contentListRepository: ContentListRepository
) {

    fun subscribe(query: String = ""): Flow<List<ContentItem>> {
        return contentListRepository.observeFavorites(query)
    }
}