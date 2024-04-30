package io.silv.movie.data.content.lists.interactor

import io.github.jan.supabase.gotrue.Auth
import io.silv.movie.data.content.lists.ContentList
import io.silv.movie.data.content.lists.repository.ContentListRepository
import io.silv.movie.data.content.lists.toUpdate
import io.silv.movie.data.user.model.list.toUserListUpdate
import io.silv.movie.data.user.repository.ListRepository
import okio.IOException


class EditContentList(
    private val network: ListRepository,
    private val local: ContentListRepository,
    private val auth: Auth,
) {

    suspend fun await(
        list: ContentList,
        update: (prev: ContentList) -> ContentList
    ): Result<ContentList> {
        val new = update(list)

        if (new.supabaseId != null && auth.currentUserOrNull()?.id == list.createdBy) {
            val result = network.updateList(new.toUserListUpdate())
            if (!result) {
                return Result.failure(IOException("Failed to remove from network"))
            }
        }
        local.updateList(new.toUpdate())

        return Result.success(new)
    }
}




