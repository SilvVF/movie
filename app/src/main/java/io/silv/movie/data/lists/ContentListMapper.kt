package io.silv.movie.data.lists

object ContentListMapper {

    val mapList = { id: Long, name: String ->
        ContentList(id, name)
    }

    val mapListItem = {
            movie_id: Long?,
            show_id: Long?,
            list_id: Long,
            contentType: String,
            title: String?,
            posterUrl: String?,
            posterLastUpdated: Long?,
            favorite: Boolean?,
            listName: String ->
        ContentListItem(
            contentId = if (contentType == "movie") movie_id!! else show_id!!,
            isMovie = contentType == "movie",
            title = title ?: "",
            posterUrl = posterUrl,
            posterLastUpdated = posterLastUpdated ?: 0L,
            favorite = favorite ?: false,
            list = ContentList(list_id, listName)
        )
    }
}