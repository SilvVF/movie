package io.silv.movie.data.content.lists.interactor

import io.github.jan.supabase.gotrue.Auth
import io.silv.movie.data.content.lists.ContentItem
import io.silv.movie.data.content.lists.ContentList
import io.silv.movie.data.content.lists.repository.ContentListRepository
import io.silv.movie.data.user.repository.ListRepository
import okio.IOException

class AddContentItemToList(
    private val network: ListRepository,
    private val local: ContentListRepository,
    private val auth: Auth,
) {

    suspend fun await(
        contentItem: ContentItem,
        list: ContentList
    ): Result<Unit> {

        if (auth.currentUserOrNull()?.id != list.createdBy && list.createdBy != null) {
            return Result.failure(IOException("Unable to edit unowned list"))
        }

        if (list.supabaseId != null) {
            val result = if (contentItem.isMovie) {
                network.addMovieToList(contentItem.contentId, contentItem.posterUrl, contentItem.title, list)
            } else {
                network.addShowToList(contentItem.contentId, contentItem.posterUrl, contentItem.title, list)
            }

            if (!result) {
                return Result.failure(IOException("Failed to remove from network"))
            }
        }

        if (contentItem.isMovie) {
            local.addMovieToList(contentItem.contentId, list, null)
        } else {
            local.addShowToList(contentItem.contentId, list, null)
        }

        return Result.success(Unit)
    }
}

