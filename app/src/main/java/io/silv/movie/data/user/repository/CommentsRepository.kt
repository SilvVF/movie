package io.silv.movie.data.user.repository

import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Count
import io.github.jan.supabase.postgrest.query.Order
import io.silv.movie.data.user.SupabaseConstants.CLIKES
import io.silv.movie.data.user.SupabaseConstants.COMMENT
import io.silv.movie.data.user.SupabaseConstants.REPLY
import io.silv.movie.data.user.SupabaseConstants.USERS
import io.silv.movie.data.user.model.comment.CLike
import io.silv.movie.data.user.model.comment.CommentWithUser
import io.silv.movie.data.user.model.comment.ReplyWithUser
import io.silv.movie.data.user.model.comment.SendComment
import io.silv.movie.data.user.model.comment.SendReply
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class CommentsRepository(
    private val postgrest: Postgrest,
    private val auth: Auth,
) {
    suspend fun unlikeComment(commentId: Long): Result<CLike> = withContext(Dispatchers.IO) {
        runCatching {
            postgrest[CLIKES]
                .delete {
                    select()
                    filter {
                        eq("cid", commentId)
                        eq("user_id", auth.currentUserOrNull()!!.id)
                    }
                }
                .decodeSingle<CLike>()
        }
    }

    suspend fun sendReply(commentId: Long, message: String) = withContext(Dispatchers.IO) {
        runCatching {

            if (message.isEmpty())
                error("text empty")

            postgrest[REPLY]
                .insert(
                    SendReply(message, commentId)
                )
        }
    }

    suspend fun getMostRecentComment(
        movieId: Long = -1,
        showId: Long = -1
    ): Result<Pair<Long,CommentWithUser>> = withContext(Dispatchers.IO) {
        runCatching {
            val result = postgrest[COMMENT]
                .select(
                    columns = Columns.raw(
                        "id, created_at, user_id, message, $USERS:$USERS!${COMMENT}_user_id_fkey(username, profile_image)"
                    )
                ) {
                    count(Count.ESTIMATED)
                    filter {
                        eq("show_id", showId)
                        eq("movie_id", movieId)
                    }
                    order("created_at", Order.DESCENDING)
                    limit(1)
                }

            result.countOrNull()!! to result.decodeSingle<CommentWithUser>()
        }
    }

    suspend fun sendComment(message: String, movieId: Long = -1, showId: Long = -1) = withContext(Dispatchers.IO) {
        runCatching {

            if (message.isEmpty())
                error("text empty")

            if (movieId == -1L && showId == -1L)
                error("invalid content id's")

            postgrest[COMMENT]
                .insert(
                    SendComment(
                        message = message,
                        movieId = movieId,
                        showId = showId,
                    )
                )
        }
    }

    suspend fun likeComment(commentId: Long): Result<CLike> = withContext(Dispatchers.IO) {
        runCatching {
            postgrest[CLIKES]
                .insert(
                    CLike(
                        userId = auth.currentUserOrNull()!!.id,
                        cid = commentId
                    )
                ) {
                    select()
                }
                .decodeSingle<CLike>()
        }
    }


    suspend fun getRepliesForComment(commentId: Long): Result<List<ReplyWithUser>> = withContext(Dispatchers.IO) {
        runCatching {
            postgrest[REPLY]
                .select(
                    columns = Columns.raw("*, $USERS:$USERS!${REPLY}_user_id_fkey(username, profile_image)")
                ) {
                    order(column = "created_at", order = Order.DESCENDING)
                    filter {
                        eq("cid", commentId)
                    }
                }
                .decodeList<ReplyWithUser>()
        }
    }
}