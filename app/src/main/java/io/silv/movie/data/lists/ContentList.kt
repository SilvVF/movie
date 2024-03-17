package io.silv.movie.data.lists

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ContentList(
    val id: Long,
    val name: String,
    val lastModified: Long,
): Parcelable

fun ContentList.toUpdate(): ContentListUpdate {
    return ContentListUpdate(id, name)
}

data class ContentListUpdate(
    val id: Long,
    val name: String?
)

data class ContentItem(
    val contentId: Long,
    val isMovie: Boolean,
    val title: String,
    val posterUrl: String?,
    val posterLastUpdated: Long,
    val favorite: Boolean,
    val lastModified: Long,
    val description: String,
    val popularity: Double,
) {

    val itemKey by lazy { "$isMovie$contentId" }
}

sealed class ContentListItem(
    open val list: ContentList
) {
    data class Item(
        val contentItem: ContentItem,
        override val list: ContentList
    ): ContentListItem(list)

    data class PlaceHolder(override val list: ContentList): ContentListItem(list)
}