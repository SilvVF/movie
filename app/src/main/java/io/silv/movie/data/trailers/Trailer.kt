package io.silv.movie.data.trailers

data class Trailer(
    val id: Long,
    val isMovie: Boolean,
    val contentId: Long,
    val trailerId: String,
    val key: String,
    val name: String,
    val official: Boolean,
    val publishedAt: String,
    val site: String,
    val size: Int,
    val type: String
)

fun io.silv.movie.core.STrailer.toDomain(): Trailer {
    return Trailer(
        id = -1L,
        isMovie = true,
        contentId = -1L,
        trailerId = trailerId,
        key = key,
        name = name,
        official = official,
        publishedAt = publishedAt,
        site = site,
        size = size,
        type = type,
    )
}

data class TrailerUpdate(
    val id: Long,
    val showId: Long?,
    val movieId: Long?,
    val trailerId: String?,
    val key: String?,
    val name: String?,
    val official: Boolean?,
    val publishedAt: String?,
    val site: String?,
    val size: Int?,
    val type: String?
)

fun Trailer.toTrailerUpdate(): TrailerUpdate {
    return TrailerUpdate(
        id = id,
        showId = contentId.takeIf { !isMovie },
        movieId =  contentId.takeIf { isMovie },
        trailerId = trailerId,
        key = key,
        name = name,
        official = official,
        publishedAt = publishedAt,
        site = site,
        size = size,
        type = type,
    )
}

