package io.silv.data.movie.model

import android.os.Parcelable
import androidx.compose.runtime.Stable
import io.silv.core.SMovie
import kotlinx.parcelize.Parcelize

@Stable
@Parcelize
data class Movie(
    val id: Long,
    val title: String,
    val overview: String,
    val genres: List<String>,
    val genreIds: List<Int>,
    val originalLanguage: String,
    val popularity: Double,
    val voteCount: Int,
    val releaseDate: String,
    val externalUrl: String,
    val posterUrl: String?,
    val posterLastUpdated: Long,
    val favorite: Boolean
): Parcelable {

    companion object {
        fun create() = Movie(
            id = -1L,
            posterUrl = "",
            title = "",
            favorite = false,
            posterLastUpdated = -1L,
            externalUrl = "",
            overview = "",
            genres = emptyList(),
            originalLanguage = "",
            popularity = 0.0,
            voteCount = 0,
            releaseDate = "",
            genreIds = emptyList()
       )
    }
}

fun SMovie.toDomain(): Movie {
    return Movie.create().copy(
        id = id,
        title = title,
        posterUrl = posterPath,
        overview = overview,
        genres =  genres?.map { it.second } ?: emptyList(),
        genreIds = genreIds ?: genres?.map { it.first } ?: emptyList(),
        originalLanguage = originalLanguage,
        popularity = popularity,
        voteCount = voteCount,
        releaseDate = releaseDate,
        externalUrl = url,
    )
}


