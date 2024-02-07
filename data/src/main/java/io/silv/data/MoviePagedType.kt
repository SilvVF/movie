package io.silv.data

import androidx.compose.runtime.Stable


@Stable
sealed interface MoviePagedType {

    sealed class Default(val name: String): MoviePagedType {
        data object Popular: Default("popular")
        data object TopRated: Default("top_rated")
        data object Upcoming: Default("upcoming")
    }

    data class Search(val query: String): MoviePagedType
}
