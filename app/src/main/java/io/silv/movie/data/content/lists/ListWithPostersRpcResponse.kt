package io.silv.movie.data.content.lists

import io.silv.movie.data.content.lists.repository.ContentListRepository
import io.silv.movie.data.content.movie.interactor.GetMovie
import io.silv.movie.data.content.tv.interactor.GetShow
import io.silv.movie.presentation.screenmodel.ListPreviewItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ListWithPostersRpcResponse(
    @SerialName("created_at")
    val createdAt: String = "",
    @SerialName("description")
    val description: String = "",
    @SerialName("list_id")
    val listId: String = "",
    @SerialName("name")
    val name: String = "",
    @SerialName("public")
    val `public`: Boolean = false,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("user_id")
    val userId: String = "",
    val username: String,
    @SerialName("profile_image")
    val profileImagePath: String?,
    @SerialName("ids")
    val ids: List<String?>? = listOf(),
    @SerialName("total")
    val total: Long? = null
) {

    val content
        get() = ids?.map {
            val split = it.orEmpty().split(',')
            val (showId, movieId) = split
            val isMovie = movieId != "-1"

            Triple(
               isMovie,
               if(isMovie) movieId.toLong() else showId.toLong(),
               split.last().takeIf { it != movieId }
            )
        } ?: emptyList()
}

suspend fun ListWithPostersRpcResponse.toListPreviewItem(
    contentListRepository: ContentListRepository,
    getShow: GetShow,
    getMovie: GetMovie,
    scope: CoroutineScope,
): ListPreviewItem {
    val item = this
    return ListPreviewItem(
        list = contentListRepository.getListForSupabaseId(item.listId)
            ?: ContentList.create().copy(
                supabaseId = item.listId,
                description = item.description,
                createdBy = item.userId,
                public = item.public,
                name = item.name,
                username = item.username,
            ),
        profileImage = item.profileImagePath,
        username = item.username,
        items = item.content.map { (isMovie, contentId, posterPath) ->

            val defItem by lazy {
                ContentItem.create().copy(
                    contentId = contentId,
                    isMovie = isMovie,
                    posterUrl = posterPath,
                )
            }

            val contentItem = if (isMovie)
                getMovie.subscribePartialOrNull(contentId).map {
                    it?.toContentItem() ?: defItem
                }
            else
                getShow.subscribePartialOrNull(contentId).map {
                    it?.toContentItem() ?: defItem
                }


            contentItem.stateIn(scope, SharingStarted.WhileSubscribed(), contentItem.first())
        }
    )
}
