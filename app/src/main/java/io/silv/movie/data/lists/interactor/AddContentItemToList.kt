package io.silv.movie.data.lists.interactor

import io.silv.movie.data.lists.ContentItem
import io.silv.movie.data.lists.ContentList
import io.silv.movie.data.lists.ContentListRepository
import io.silv.movie.data.user.ListRepository
import okio.IOException

class AddContentItemToList(
    private val network: ListRepository,
    private val local: ContentListRepository,
) {

    suspend fun await(
        contentItem: ContentItem,
        list: ContentList
    ): Result<Unit> {
        if (list.supabaseId != null) {
            val result = if (contentItem.isMovie) {
                network.addMovieToList(contentItem.contentId, contentItem.posterUrl, list)
            } else {
                network.addShowToList(contentItem.contentId, contentItem.posterUrl, list)
            }

            if (!result) {
                return Result.failure(IOException("Failed to remove from network"))
            }
        }

        if (contentItem.isMovie) {
            local.addMovieToList(contentItem.contentId, list)
        } else {
            local.addShowToList(contentItem.contentId, list)
        }

        return Result.success(Unit)
    }
}

