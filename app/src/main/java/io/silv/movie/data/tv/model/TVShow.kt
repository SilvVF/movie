package io.silv.movie.data.tv.model

import android.os.Parcelable
import androidx.compose.runtime.Stable
import io.silv.movie.core.STVShow
import kotlinx.datetime.Clock
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Stable
@Parcelize
data class TVShowPoster(
    val id: Long,
    val title: String,
    val overview: String,
    val posterLastUpdated: Long,
    val posterUrl: String?,
    val favorite: Boolean,
    val inLibraryLists: Long
): Parcelable

@Stable
@Parcelize
data class TVShow(
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
    val favorite: Boolean,
    val inLists: Int,
    val status: io.silv.movie.core.Status?,
    val productionCompanies: List<String>?,
    val lastModifiedAt: Long,
    val favoriteLastModified: Long
): Parcelable {
    @IgnoredOnParcel
    val needsInit by lazy { status == null || productionCompanies == null }

    val inList: Boolean
        get() = inLists >= 1

    companion object {
        fun create() = TVShow(
            id = -1L,
            posterUrl = "",
            title = "",
            favorite = false,
            posterLastUpdated = Clock.System.now().epochSeconds,
            externalUrl = "",
            overview = "",
            genres = emptyList(),
            originalLanguage = "",
            popularity = 0.0,
            voteCount = 0,
            releaseDate = "",
            genreIds = emptyList(),
            status = null,
            productionCompanies = emptyList(),
            lastModifiedAt = 0L,
            inLists = 0,
            favoriteLastModified = 0L
        )
    }
}

fun STVShow.toDomain(): TVShow {
    return TVShow.create().copy(
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
        status = status
    )
}