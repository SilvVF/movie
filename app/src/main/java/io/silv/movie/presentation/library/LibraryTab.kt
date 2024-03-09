package io.silv.movie.presentation.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.LibraryBooks
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import io.silv.movie.data.movie.model.Movie
import io.silv.movie.data.tv.TVShow
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

class LibraryScreenModel(

): StateScreenModel<LibraryState>(LibraryState()) {

}

data class LibraryState(
    val movies: ImmutableList<Movie> = persistentListOf(),
    val shows: ImmutableList<TVShow> = persistentListOf()
)

object LibraryTab: Tab {

    override val options: TabOptions
        @Composable get() = TabOptions(
            index = 1u,
            title = "Library",
            icon = rememberVectorPainter(image = Icons.AutoMirrored.Rounded.LibraryBooks)
        )

    @Composable
    override fun Content() {
        LibraryStandardScreenContent()
    }
}

@Composable
fun LibraryStandardScreenContent() {
    Box(Modifier.fillMaxSize()) {
        Text("Library", Modifier.align(Alignment.Center))
    }
}