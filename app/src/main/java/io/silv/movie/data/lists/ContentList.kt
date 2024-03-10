package io.silv.movie.data.lists

data class ContentList(
    val id: Long,
    val name: String
)

fun ContentList.toUpdate(): ContentListUpdate {
    return ContentListUpdate(id, name)
}

data class ContentListUpdate(
    val id: Long,
    val name: String?
)

sealed class ContentListItem(
    open val list: ContentList
) {
    data class Item(
        val contentId: Long,
        val isMovie: Boolean,
        val title: String,
        val posterUrl: String?,
        val posterLastUpdated: Long,
        val favorite: Boolean,
        override val list: ContentList
    ): ContentListItem(list)

    data class PlaceHolder(override val list: ContentList): ContentListItem(list)
}