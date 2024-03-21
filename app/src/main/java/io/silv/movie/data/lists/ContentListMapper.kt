package io.silv.movie.data.lists

object ContentListMapper {

    val mapList = {
        _id: Long,
        supabase_id: String?,
        created_by: String?,
        name: String,
        last_modified_at: Long,
        poster_last_updated: Long? ->
        ContentList(
            _id,
            name,
            last_modified_at,
            poster_last_updated ?: -1L
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
        inList: Boolean?
        ->
        ContentItem(
            contentId = movieId ?: showId!!,
            isMovie = movieId != null,
            title = title ?: "",
            posterUrl = posterUrl,
            posterLastUpdated = posterLastUpdated ?: -1L,
            favorite = favorite ?: false,
            lastModified = last_modified_at ?: -1L,
            popularity = popularity ?: 0.0,
            description = overview ?: "",
            inList = inList ?: false
        )
    }

    val mapFavoriteItem = {
            id: Long, title: String, poster_url: String?,
            poster_last_updated: Long, overview: String, popularity: Double,
            last_modified_at: Long,favorite: Boolean,  isMovie: Long,
            favorite_last_modified: Long? ->
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
            inList = true
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
            inList: Boolean? -> ContentItem(
                contentId = showId ?: movieId!!,
                isMovie = movieId != null,
                title = title ?: "",
                posterUrl = posterUrl,
                posterLastUpdated = posterLastUpdated ?: 0L,
                favorite = favorite ?: false,
                lastModified = created_at,
                popularity = popularity ?: 0.0,
                description = overview ?: "",
                inList = inList ?: false
            )
    }

    val mapListItem = {
            list_id: Long,
            list_name: String,
            lastModified: Long,
            listPosterLastUpdated: Long?,
            movieId: Long?,
            showId: Long?,
            addedToListAt: Long?,
            title: String?,
            posterUrl: String?,
            posterLastUpdated: Long?,
            favorite: Boolean?,
            overview: String?,
            popularity: Double?,
            inList: Boolean?    ->
        if(movieId != null || showId != null) {
            ContentListItem.Item(
               contentItem = ContentItem(
                   contentId = showId ?: movieId!!,
                   isMovie = movieId != null,
                   title = title ?: "",
                   posterUrl = posterUrl,
                   posterLastUpdated = posterLastUpdated ?: 0L,
                   favorite = favorite ?: false,
                   lastModified =  addedToListAt ?: 0L,
                   popularity = popularity ?: 0.0,
                   description = overview ?: "",
                   inList = inList ?: false
               ),
               list = ContentList(
                   list_id,
                   list_name,
                   lastModified,
                   listPosterLastUpdated ?: -1L
               )
            )
        } else {
            ContentListItem.PlaceHolder(
                ContentList(
                    list_id,
                    list_name,
                    lastModified,
                    listPosterLastUpdated ?: -1L)
            )
        }
    }
}