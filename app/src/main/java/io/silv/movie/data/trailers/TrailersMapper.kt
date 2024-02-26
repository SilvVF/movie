package io.silv.movie.data.trailers

object TrailersMapper {

    val mapTrailer = {
            _id: Long,
            trailer_id: String,
            movie_id : Long,
            name: String,
            video_key: String,
            site: String,
            size: Long,
            official: Boolean,
            type: String,
            published_at: String ->
        Trailer(
            id = _id,
            movieId = movie_id,
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