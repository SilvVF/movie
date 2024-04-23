package io.silv.movie.data.lists

import android.os.Parcelable
import io.silv.movie.data.movie.model.Movie
import io.silv.movie.data.movie.model.MoviePoster
import io.silv.movie.data.tv.model.TVShow
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
    val username: String,
    val description: String,
    val lastModified: Long,
    val posterLastModified: Long,
    val createdAt: Long,
    val inLibrary: Boolean,
    val subscribers: Long,
): Parcelable {

    companion object {
        fun create(): ContentList  = ContentList(
            id = -1,
            supabaseId = null,
            createdBy = null,
            lastSynced = null,
            public = false,
            name = "",
            username = "",
            description = "",
            lastModified = -1L,
            posterLastModified = -1L,
            createdAt = -1L,
            inLibrary = false,
            subscribers = -1L
        )
    }
}

fun ContentList.toUpdate(): ContentListUpdate {
    return ContentListUpdate(
        id = id,
        name = name,
        username = username,
        description = description,
        posterLastUpdated = posterLastModified,
        inLibrary = inLibrary,
        public = public,
        subscribers = subscribers
    )
}

data class ContentListUpdate(
    val id: Long,
    val name: String? = null,
    val username: String? = null,
    val description: String? = null,
    val posterLastUpdated: Long? = null,
    val inLibrary: Boolean? = null,
    val public: Boolean? = null,
    val subscribers: Long? = null,
)

fun Movie.toContentItem(): ContentItem {
    val it = this
    return ContentItem(
        title = it.title,
        isMovie = true,
        contentId = it.id,
        posterUrl = it.posterUrl,
        posterLastUpdated = it.posterLastUpdated,
        favorite = it.favorite,
        lastModified = it.lastModifiedAt,
        description = it.overview,
        popularity = it.popularity,
        inLibraryLists = it.inLists.toLong()
    )
}

fun TVShow.toContentItem(): ContentItem {
    val it = this
    return ContentItem(
        title = it.title,
        isMovie = false,
        contentId = it.id,
        posterUrl = it.posterUrl,
        posterLastUpdated = it.posterLastUpdated,
        favorite = it.favorite,
        lastModified = it.lastModifiedAt,
        description = it.overview,
        popularity = it.popularity,
        inLibraryLists = it.inLists.toLong()
    )
}

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
        description = it.overview,
        popularity = 0.0,
        inLibraryLists = it.inLibraryLists
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
        description = it.overview,
        popularity = 0.0,
        inLibraryLists = it.inLibraryLists
    )
}

data class ContentItem(
    val contentId: Long,
    val isMovie: Boolean,
    val title: String,
    val posterUrl: String?,
    val posterLastUpdated: Long,
    val favorite: Boolean,
    val inLibraryLists: Long,
    val lastModified: Long,
    val description: String,
    val popularity: Double,
) {
    val inLibraryList
        get() = inLibraryLists >= 1L

    val itemKey by lazy { "$isMovie$contentId" }

    companion object {
        fun create() = ContentItem(
            contentId = -1,
            isMovie = false,
            title = "",
            posterUrl = null,
            posterLastUpdated = -1L,
            favorite = false,
            inLibraryLists = -1,
            lastModified = -1L,
            description = "",
            popularity = -1.0
        )
    }
}

sealed class ContentListItem(
    open val list: ContentList
) {
    data class Item(
        val contentItem: ContentItem,
        val createdAt: Long,
        override val list: ContentList
    ): ContentListItem(list)

    data class PlaceHolder(override val list: ContentList): ContentListItem(list)
}