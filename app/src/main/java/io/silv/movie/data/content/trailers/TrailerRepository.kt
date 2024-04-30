package io.silv.movie.data.content.trailers

interface TrailerRepository {
    suspend fun insertTrailer(trailer: Trailer, contentId: Long, isMovie: Boolean)
    suspend fun updateTrailer(update: TrailerUpdate): Boolean
    suspend fun getById(id: String): Trailer?
    suspend fun getTrailersByMovieId(movieId: Long): List<Trailer>
    suspend fun getTrailersByShowId(showId: Long): List<Trailer>
}

class TrailerRepositoryImpl(
    private val handler: io.silv.movie.database.DatabaseHandler
): TrailerRepository {

    override suspend fun insertTrailer(trailer: Trailer, contentId: Long, isMovie: Boolean) {
        handler.await {
            trailerQueries.insert(
                trailerId = trailer.id,
                movieId = contentId.takeIf { isMovie },
                showId = contentId.takeIf { !isMovie },
                name = trailer.name,
                videoKey = trailer.key,
                site = trailer.site,
                size = trailer.size.toLong(),
                official = trailer.official,
                type = trailer.type,
                publishedAt = trailer.publishedAt
            )
        }
    }

    override suspend fun getTrailersByMovieId(movieId: Long): List<Trailer> {
        return handler.awaitList { trailerQueries.selectByMovieId(movieId, TrailersMapper.mapTrailer)}
    }

    override suspend fun getTrailersByShowId(showId: Long): List<Trailer> {
        return handler.awaitList { trailerQueries.selectByShowId(showId, TrailersMapper.mapTrailer)}
    }


    override suspend fun updateTrailer(update: TrailerUpdate): Boolean {
        return runCatching {
            partialUpdateTrailer(update)
        }
            .isSuccess
    }

    override suspend fun getById(id: String): Trailer? {
        return handler.awaitOneOrNull { trailerQueries.selectById(id, TrailersMapper.mapTrailer) }
    }

    private suspend fun partialUpdateTrailer(vararg updates: TrailerUpdate) {
        return handler.await {
            updates.forEach { update ->
                trailerQueries.update(
                    id  = update.id,
                    name = update.name,
                    videoKey = update.key,
                    site = update.site,
                    size = update.size?.toLong(),
                    official = update.official,
                    type = update.type,
                    publishedAt = update.publishedAt
                )
            }
        }
    }
}