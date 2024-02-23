package io.silv.data.trailers

import io.silv.core.STrailer

data class Trailer(
    val id: Long,
    val movieId: Long,
    val trailerId: String,
    val key: String,
    val name: String,
    val official: Boolean,
    val publishedAt: String,
    val site: String,
    val size: Int,
    val type: String
)

fun STrailer.toDomain(): Trailer {
    return Trailer(
        id = -1L,
        movieId = -1L,
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
        movieId = movieId,
        trailerId = trailerId,
        key = key,
        name = name,
        official = official,
        publishedAt = publishedAt,
        site = site,
        size = size,
        type = type
    )
}

