package io.silv.movie.data.lists

object ContentListMapper {

    val mapList = {
            _id: Long,
            supabase_id: String?,
            created_by: String?,
            username: String,
            description: String,
            synced_at: Long?,
            public: Boolean,
            name: String,
            last_modified_at: Long,
            poster_last_updated: Long?,
            created_at: Long,
            in_library: Boolean ->
        ContentList(
            id = _id,
            supabaseId = supabase_id,
            createdBy = created_by,
            lastSynced = synced_at,
            public = public,
            name = name,
            description = description,
            lastModified = last_modified_at,
            posterLastModified = poster_last_updated ?: -1L,
            createdAt = created_at,
            username = username,
            inLibrary = in_library
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
        last_modified_at: Long?,
        inLists: Long?
        ->
        ContentItem(
            contentId = movieId.takeIf { it != -1L } ?: showId!!,
            isMovie = movieId.takeIf { it != -1L } != null,
            title = title ?: "",
            posterUrl = posterUrl,
            posterLastUpdated = posterLastUpdated ?: -1L,
            favorite = favorite ?: false,
            lastModified = last_modified_at ?: -1L,
            popularity = popularity ?: 0.0,
            description = overview ?: "",
            inLibraryLists = inLists ?: 0L
        )
    }

    val mapFavoriteItem = {
            id: Long, title: String, poster_url: String?,
            poster_last_updated: Long, overview: String, popularity: Double,
            last_modified_at: Long,favorite: Boolean,  isMovie: Long,
            favorite_last_modified: Long?,
        inLibraryLists: Long ->
        ContentItem(
            contentId = id,
            isMovie = isMovie == 1L,
            title = title,
            posterUrl = poster_url,
            posterLastUpdated = poster_last_updated,
            favorite = favorite,
            lastModified = last_modified_at,
            popularity = popularity,
            description = overview,
            inLibraryLists = inLibraryLists
        )
    }

    val mapItem = {
            movie_id: Long?,
            show_id: Long?,
            list_id: Long,
            created_at: Long,
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
                lastModified = created_at,
                popularity = popularity ?: 0.0,
                description = overview ?: "",
                inLibraryLists = inLists ?: 0L
            )
    }

    val mapListItem = {
            list_id: Long,
            supabase_id: String?,
            created_by: String?,
            username: String,
            description: String,
            synced_at: Long?,
            public_: Boolean,
            name: String,
            last_modified_at: Long,
            poster_last_updated: Long?,
            created_at: Long,
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
            inLists: Long?  ->
        if(movieId != null || showId != null) {
            ContentListItem.Item(
               contentItem = ContentItem(
                   contentId = showId.takeIf { it != -1L } ?: movieId!!,
                   isMovie = movieId.takeIf { it != -1L } != null,
                   title = title ?: "",
                   posterUrl = posterUrl,
                   posterLastUpdated = posterLastUpdated ?: 0L,
                   favorite = favorite ?: false,
                   lastModified =  addedToListAt ?: 0L,
                   popularity = popularity ?: 0.0,
                   description = overview ?: "",
                   inLibraryLists = inLists ?: 0L
               ),
               list =  ContentList(
                   id = list_id,
                   supabaseId = supabase_id,
                   createdBy = created_by,
                   lastSynced = synced_at,
                   public = public_,
                   name = name,
                   description = description,
                   lastModified = last_modified_at,
                   posterLastModified = poster_last_updated ?: -1L,
                   username = username,
                   createdAt = created_at,
                   inLibrary = inLibrary
               )
            )
        } else {
            ContentListItem.PlaceHolder(
                ContentList(
                    id = list_id,
                    supabaseId = supabase_id,
                    createdBy = created_by,
                    lastSynced = synced_at,
                    public = public_,
                    name = name,
                    description = description,
                    lastModified = last_modified_at,
                    posterLastModified = poster_last_updated ?: -1L,
                    username = username,
                    inLibrary = inLibrary,
                    createdAt = created_at
                )
            )
        }
    }
}