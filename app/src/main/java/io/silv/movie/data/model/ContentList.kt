package io.silv.movie.data.model

import android.os.Parcelable
import io.silv.movie.data.supabase.ContentType
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
    val pinned: Boolean
): Parcelable {

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (supabaseId?.hashCode() ?: 0)
        result = 31 * result + (createdBy?.hashCode() ?: 0)
        result = 31 * result + (lastSynced?.hashCode() ?: 0)
        result = 31 * result + public.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + username.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + lastModified.hashCode()
        result = 31 * result + posterLastModified.hashCode()
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + inLibrary.hashCode()
        result = 31 * result + subscribers.hashCode()
        result = 31 * result + pinned.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ContentList

        if (id != other.id) return false
        if (supabaseId != other.supabaseId) return false
        if (createdBy != other.createdBy) return false
        if (lastSynced != other.lastSynced) return false
        if (public != other.public) return false
        if (name != other.name) return false
        if (username != other.username) return false
        if (description != other.description) return false
        if (lastModified != other.lastModified) return false
        if (posterLastModified != other.posterLastModified) return false
        if (createdAt != other.createdAt) return false
        if (inLibrary != other.inLibrary) return false
        if (subscribers != other.subscribers) return false
        if (pinned != other.pinned) return false

        return true
    }

    companion object {
        fun create(): ContentList = ContentList(
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
            subscribers = -1L,
            pinned = false
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
        subscribers = subscribers,
        pinned = pinned
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
    val pinned: Boolean? = null
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

    fun toType(): ContentType = when (isMovie) {
        true -> ContentType.Movie
        false -> ContentType.Show
    }

    companion object {
        fun create() = ContentItem(
            contentId = -1,
            isMovie = false,
            title = "",
            posterUrl = null,
            posterLastUpdated = 0L,
            favorite = false,
            inLibraryLists = 0,
            lastModified = 0L,
            description = "",
            popularity = 0.0
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