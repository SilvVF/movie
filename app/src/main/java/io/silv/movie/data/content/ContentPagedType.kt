package io.silv.movie.data.content

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.input.KeyboardType
import java.io.Serializable


@Stable
sealed interface ContentPagedType: Serializable {

    @Stable
    data class Discover(
       val filters: Filters
    ): ContentPagedType

    @Stable
    sealed class Default(val name: String): ContentPagedType {
        data object Popular: Default("popular")
        data object TopRated: Default("top_rated")
        data object Upcoming: Default("upcoming")
    }

    @Stable
    data class Search(val query: String): ContentPagedType
}

@Stable
data class Filters(
    val genres: List<Genre>,
    val genreMode: GenreMode,
    val sortingOption: SortingOption,
    val companies: MutableState<String>,
    val keywords: MutableState<String>,
    val people: MutableState<String>,
    val year: MutableState<String>,
    val voteCount: MutableState<String>,
    val voteAverage: MutableState<String>
): Serializable {
    companion object {
        val default = Filters(
            genres = listOf(),
            genreMode = GenreMode.And,
            sortingOption = SortingOption.PopularityDesc,
            companies = mutableStateOf(""),
            keywords = mutableStateOf(""),
            people = mutableStateOf(""),
            year = mutableStateOf(""),
            voteCount = mutableStateOf(""),
            voteAverage = mutableStateOf("")
        )
    }
}

@Stable
enum class SortingOption(val title: String, val sort: String): Serializable {
    PopularityAsc("Popularity asc", "popularity.asc"),
    PopularityDesc("Popularity desc", "popularity.desc"),
    TitleAsc("Title asc", "title.asc"),
    TitleDesc("Title desc", "title.desc"),
    RevenueAsc("Revenue asc", "revenue.asc"),
    RevenueDesc("Revenue desc", "revenue.desc"),
    VoteCountAsc("Vote count asc", "vote_count.asc"),
    VoteCountDesc("Vote count desc", "vote_count.desc"),
    VoteAverageAsc("Vote average asc", "vote_average.asc"),
    VoteAverageDsc("Vote average desc", "vote_average.desc")
}

@Stable
sealed interface GenreMode: Serializable {
    data object Or: GenreMode
    data object And: GenreMode
}

@Stable
data class SearchItem(
    val text: MutableState<String>,
    val label: String,
    val error: State<String?> = mutableStateOf(null),
    val placeHolder: String = "",
    val keyboardType: KeyboardType = KeyboardType.Text
)


