package io.silv.movie.data

import io.github.jan.supabase.auth.Auth
import io.silv.movie.data.model.ContentList
import io.silv.movie.data.local.ContentListRepository
import io.silv.movie.data.model.toUpdate
import io.silv.movie.data.supabase.model.list.toUserListUpdate
import io.silv.movie.data.supabase.ListRepository
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




