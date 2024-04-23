package io.silv.movie.data.lists

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
            inLibrary: Boolean ->
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
            inLibrary = inLibrary
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
            id: Long, title: String, posterUrl: String?,
            posterLastUpdated: Long, overview: String, popularity: Double,
            lastModifiedAt: Long,favorite: Boolean,  isMovie: Long,
            _: Long?,
        inLibraryLists: Long ->
        ContentItem(
            contentId = id,
            isMovie = isMovie == 1L,
            title = title,
            posterUrl = posterUrl,
            posterLastUpdated = posterLastUpdated,
            favorite = favorite,
            lastModified = lastModifiedAt,
            popularity = popularity,
            description = overview,
            inLibraryLists = inLibraryLists
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

        val contentList = ContentList(
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
            inLibrary = inLibrary
        )

        if(movieId != null || showId != null) {
            ContentListItem.Item(
               contentItem = ContentItem(
                   contentId = showId.takeIf { it != -1L } ?: movieId!!,
                   isMovie = movieId.takeIf { it != -1L } != null,
                   title = title ?: "",
                   posterUrl = posterUrl,
                   posterLastUpdated = posterLastUpdated ?: 0L,
                   favorite = favorite ?: false,
                   lastModified =  contentLastModified ?: 0L,
                   popularity = popularity ?: 0.0,
                   description = overview ?: "",
                   inLibraryLists = inLists ?: 0L
               ),
               createdAt = addedToListAt ?: 0L,
               list =  contentList
            )
        } else {
            ContentListItem.PlaceHolder(contentList)
        }
    }
}