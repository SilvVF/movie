package io.silv.data.movie.model

import androidx.compose.runtime.Stable


@Stable
sealed interface ContentPagedType {

    @Stable
    data class Discover(val genres: List<String>): ContentPagedType

    @Stable
    sealed class Default(val name: String): ContentPagedType {
        data object Popular: Default("popular")
        data object TopRated: Default("top_rated")
        data object Upcoming: Default("upcoming")
    }

    @Stable
    data class Search(val query: String): ContentPagedType
}

