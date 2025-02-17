package io.silv.movie.data.supabase.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class FavoriteMovie(
    @SerialName("user_id")
    val userId: String,
    @SerialName("poster_path")
    val posterPath: String,
    val title: String,
    val overview: String,
    @SerialName("vote_average")
    val voteAverage: Double,
    @SerialName("movie_id")
    val movieId: Long,
    @SerialName("show_id")
    val showId: Long,
    @SerialName("_id")
    val id: Long,
)

@Serializable
data class FavoriteMovieInsert(
    @SerialName("user_id")
    val userId: String,
    @SerialName("poster_path")
    val posterPath: String?,
    val title: String,
    val overview: String,
    @SerialName("vote_average")
    val voteAverage: Double,
    @SerialName("movie_id")
    val movieId: Long,
    @SerialName("show_id")
    val showId: Long,
)
