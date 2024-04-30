package io.silv.movie.data.content.trailers

object TrailersMapper {

    val mapTrailer = {
            trailer_id: String,
            show_id: Long?,
            movie_id: Long?,
            name: String,
            video_key: String,
            site: String,
            size: Long,
            official: Boolean,
            type: String,
            published_at: String ->
        Trailer(
            id = trailer_id,
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