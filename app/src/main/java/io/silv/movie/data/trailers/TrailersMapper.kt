package io.silv.movie.data.trailers

object TrailersMapper {

    val mapTrailer = {
            _id: Long,
            trailer_id: String,
            movie_id : Long?,
            show_id: Long?,
            name: String,
            video_key: String,
            site: String,
            size: Long,
            official: Boolean,
            type: String,
            published_at: String ->
        Trailer(
            id = _id,
            isMovie = movie_id != null,
            contentId = movie_id ?: show_id!!,
            trailerId = trailer_id,
            key = video_key,
            name = name,
            official = official ,
            publishedAt = published_at,
            site = site,
            size = size.toInt(),
            type = type
        )
    }
}