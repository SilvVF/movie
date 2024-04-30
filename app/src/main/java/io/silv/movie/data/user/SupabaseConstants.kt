package io.silv.movie.data.user

import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.result.PostgrestResult
import io.github.jan.supabase.postgrest.rpc
import io.silv.movie.data.user.SupabaseConstants.CommentsOrder.Newest
import io.silv.movie.data.user.SupabaseConstants.CommentsOrder.Top
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object SupabaseConstants {

    const val FAVORITE_MOVIES = "favoritemovies"
    const val USER_LIST = "userlist"
    const val LIST_ITEM = "listitem"
    const val SUBSCRIPTION = "subscription"
    const val CLIKES = "clikes"
    const val COMMENT = "comment"
    const val REPLY = "reply"
    const val USERS = "users"


    enum class CommentsOrder { Newest, Top }

    object RPC {

        suspend fun Postgrest.deleteUser(): PostgrestResult {
            return rpc("deleteUser")
        }

        suspend fun Postgrest.selectCommentsForContent(
            userId: String,
            movieId: Long = -1,
            showId: Long = -1,
            offset: Int = 0,
            limit: Int = 30,
            order: CommentsOrder = Newest,
        ): PostgrestResult {
            return rpc(
                when (order){
                    Newest -> "select_comments_for_content_with_info"
                    Top -> "select_top_comments_for_content_with_info"
                },
                SelectCommentsRpcParams(
                    uid = userId,
                    movieId = movieId,
                    showId = showId,
                    lim = limit,
                    off = offset
                )
            )
        }

        suspend fun Postgrest.moreFromSubscribed(userId: String, limit: Int = 10, offset: Int = 0): PostgrestResult {
            return rpc(
                "select_recommended_by_subscriptions",
                SelectMoreFromSubscribedParams(userId, lim = limit, off = offset)
            )
        }

        suspend fun Postgrest.subscribedListWithItems(userId: String): PostgrestResult {
            return rpc(
                "select_subscribed_lists_with_items",
                parameters = SubscriptionRpcParams(userId)
            )
        }
    }

    @Serializable
    private data class SelectCommentsRpcParams(
        val uid: String,
        @SerialName("mid")
        private val movieId: Long,
        @SerialName("sid")
        private val showId: Long,
        val lim: Int,
        val off: Int
    )

    @Serializable
    private data class SubscriptionRpcParams(val uid: String)

    @Serializable
    data class SelectMoreFromSubscribedParams(
        val uid: String,
        val lim: Int,
        val off: Int,
    )
}