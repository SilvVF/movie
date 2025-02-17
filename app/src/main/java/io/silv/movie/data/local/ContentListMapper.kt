package io.silv.movie.data.local

import io.silv.movie.data.model.ContentItem
import io.silv.movie.data.model.ContentList
import io.silv.movie.data.model.ContentListItem

object ContentListMapper {

    val mapList = {
            id: Long,
            supabaseId: String?,
            createdBy: String?,
            username: String,
            description: String,
            syncedAt: Long?,
            public: Boolean,
            name: String,
            lastModifiedAt: Long,
            posterLastUpdated: Long?,
            createdAt: Long,
            inLibrary: Boolean,
            subscribers: Long,
            pinned: Boolean ->
        ContentList(
            id = id,
            supabaseId = supabaseId,
            createdBy = createdBy,
            lastSynced = syncedAt,
            public = public,
            name = name,
            description = description,
            lastModified = lastModifiedAt,
            posterLastModified = posterLastUpdated ?: -1L,
            createdAt = createdAt,
            username = username,
            inLibrary = inLibrary,
            subscribers = subscribers,
            pinned = pinned
        )
    }

    val mapRecommendation = {
        movieId: Long?,
        showId: Long?,
        title: String?,
        posterUrl: String?,
        posterLastUpdated: Long?,
        favorite: Boolean?,
        overview: String?,
        popularity: Double?,
        lastModifiedAt: Long?,
        inLists: Long?
        ->
        ContentItem(
            contentId = movieId.takeIf { it != -1L } ?: showId!!,
            isMovie = movieId.takeIf { it != -1L } != null,
            title = title ?: "",
            posterUrl = posterUrl,
            posterLastUpdated = posterLastUpdated ?: -1L,
            favorite = favorite ?: false,
            lastModified = lastModifiedAt ?: -1L,
            popularity = popularity ?: 0.0,
            description = overview ?: "",
            inLibraryLists = inLists ?: 0L
        )
    }

    val mapFavoriteItem = {
            id: Long,
            title: String,
            poster_url: String?,
            poster_last_updated: Long?,
            overview: String,
            popularity: Double?,
            last_modified_at: Long,
            favorite: Boolean?,
            isMovie: Long,
            favorite_modified_at: Long?,
            inLists: Long ->
        ContentItem(
            contentId = id,
            isMovie = isMovie == 1L,
            title = title,
            posterUrl = poster_url,
            posterLastUpdated = poster_last_updated ?: -1L,
            favorite = favorite ?: false,
            lastModified = last_modified_at ,
            popularity = popularity ?: -1.0,
            description = overview,
            inLibraryLists = inLists
        )
    }

    val mapItem = {
            _: Long?,
            _: Long?,
            _: Long,
            createdAt: Long,
            movieId: Long?,
            showId: Long?,
            title: String?,
            posterUrl: String?,
            posterLastUpdated: Long?,
            favorite: Boolean?,
            overview: String?,
            popularity: Double?,
            inLists: Long? -> ContentItem(
                contentId = showId.takeIf { it != -1L } ?: movieId!!,
                isMovie = movieId.takeIf { it != -1L } != null,
                title = title ?: "",
                posterUrl = posterUrl,
                posterLastUpdated = posterLastUpdated ?: 0L,
                favorite = favorite ?: false,
                lastModified = createdAt,
                popularity = popularity ?: 0.0,
                description = overview ?: "",
                inLibraryLists = inLists ?: 0L
            )
    }

    val mapListItem = {
            listId: Long,
            supabaseId: String?,
            createdBy: String?,
            username: String,
            description: String,
            _: Long?,
            public: Boolean,
            name: String,
            lastModifiedAt: Long,
            _: Long?,
            createdAt: Long,
            inLibrary: Boolean,
            subscribers: Long,
            pinned: Boolean,
            movieId: Long?,
            showId: Long?,
            addedToListAt: Long?,
            title: String?,
            posterUrl: String?,
            posterLastUpdated: Long?,
            favorite: Boolean?,
            overview: String?,
            popularity: Double?,
            inLists: Long?,
            contentLastModified: Long? ->

        val list = ContentList(
            id = listId,
            supabaseId = supabaseId,
            createdBy = createdBy,
            lastSynced = createdAt,
            public = public,
            name = name,
            description = description,
            lastModified = lastModifiedAt,
            posterLastModified = posterLastUpdated ?: -1L,
            username = username,
            createdAt = createdAt,
            inLibrary = inLibrary,
            subscribers = subscribers,
            pinned = pinned
        )

        if(movieId.takeIf { it != -1L } != null || showId.takeIf { it != -1L } != null) {
            ContentListItem.Item(
                contentItem = ContentItem(
                    contentId = showId.takeIf { it != -1L } ?: movieId!!,
                    isMovie = movieId.takeIf { it != -1L } != null,
                    title = title ?: "",
                    posterUrl = posterUrl,
                    posterLastUpdated = posterLastUpdated ?: 0L,
                    favorite = favorite ?: false,
                    lastModified = contentLastModified ?: 0L,
                    popularity = popularity ?: 0.0,
                    description = overview ?: "",
                    inLibraryLists = inLists ?: 0L
                ),
                createdAt = addedToListAt ?: 0L,
                list = list
            )
        } else {
            ContentListItem.PlaceHolder(list)
        }
    }
}