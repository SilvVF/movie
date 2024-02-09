package io.silv.data.movie.model

import androidx.compose.runtime.Stable


@Stable
sealed interface MoviePagedType {

    @Stable
    data class Discover(val genres: List<String>): MoviePagedType

    @Stable
    sealed class Default(val name: String): MoviePagedType {
        data object Popular: Default("popular")
        data object TopRated: Default("top_rated")
        data object Upcoming: Default("upcoming")
    }

    @Stable
    data class Search(val query: String): MoviePagedType
}
