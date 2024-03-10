package io.silv.movie.data.lists

object ContentListMapper {

    val mapList = { id: Long, name: String, lastModified: Long ->
        ContentList(id, name, lastModified)
    }

    val mapListItem = {
            list_id: Long,
            list_name: String,
            lastModified: Long,
            movieId: Long?,
            showId: Long?,
            title: String?,
            posterUrl: String?,
            posterLastUpdated: Long?,
            favorite: Boolean? ->
        if(movieId != null || showId != null) {
            ContentListItem.Item(
                contentId = showId ?: movieId!!,
                isMovie = movieId != null,
                title = title ?: "",
                posterUrl = posterUrl,
                posterLastUpdated = posterLastUpdated ?: 0L,
                favorite = favorite ?: false,
                list = ContentList(list_id, list_name, lastModified)
            )
        } else {
            ContentListItem.PlaceHolder(ContentList(list_id, list_name, lastModified))
        }
    }
}