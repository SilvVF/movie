package io.silv.movie.data.lists

import android.os.Parcelable
import io.silv.movie.data.movie.model.MoviePoster
import io.silv.movie.data.tv.model.TVShowPoster
import kotlinx.parcelize.Parcelize

@Parcelize
data class ContentList(
    val id: Long,
    val supabaseId: String? = null,
    val createdBy: String? = null,
    val lastSynced: Long? = null,
    val public: Boolean = false,
    val name: String,
    val lastModified: Long,
    val posterLastModified: Long,
    val createdAt: Long
): Parcelable

fun ContentList.toUpdate(): ContentListUpdate {
    return ContentListUpdate(id, name, posterLastModified)
}

data class ContentListUpdate(
    val id: Long,
    val name: String? = null,
    val posterLastUpdated: Long? = null
)

fun MoviePoster.toContentItem(): ContentItem {
    val it = this
    return ContentItem(
        title = it.title,
        isMovie = true,
        contentId = it.id,
        posterUrl = it.posterUrl,
        posterLastUpdated = it.posterLastUpdated,
        favorite = it.favorite,
        lastModified = -1L,
        description = "",
        popularity = 0.0,
        inList = inList
    )
}

fun TVShowPoster.toContentItem(): ContentItem {
    val it = this
    return ContentItem(
        title = it.title,
        isMovie = false,
        contentId = it.id,
        posterUrl = it.posterUrl,
        posterLastUpdated = it.posterLastUpdated,
        favorite = it.favorite,
        lastModified = -1L,
        description = "",
        popularity = 0.0,
        inList = inList
    )
}

data class ContentItem(
    val contentId: Long,
    val isMovie: Boolean,
    val title: String,
    val posterUrl: String?,
    val posterLastUpdated: Long,
    val favorite: Boolean,
    val inList: Boolean,
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