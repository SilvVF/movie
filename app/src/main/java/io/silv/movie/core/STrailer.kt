package io.silv.movie.core

import java.io.Serializable

interface STrailer : Serializable {

    var movieId: Long
    var trailerId: String
    var key: String
    var name: String
    var official: Boolean
    var publishedAt: String
    var site: String
    var size: Int
    var type: String

    fun copy() = create().also {
        it.movieId = movieId
        it.trailerId = trailerId
        it.key = key
        it.name = name
        it.official = official
        it.publishedAt = publishedAt
        it.site = site
        it.size = size
        it.type = type
    }

    companion object {
        fun create(): STrailer {
            return STrailerImpl()
        }
    }
}

class STrailerImpl(
    override var movieId: Long = -1L,
    override var trailerId: String = "",
    override var key: String = "",
    override var name: String = "",
    override var official: Boolean = false,
    override var publishedAt: String = "",
    override var site: String = "",
    override var size: Int = -1,
    override var type: String = ""
) : STrailer