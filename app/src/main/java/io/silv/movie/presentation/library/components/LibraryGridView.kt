package io.silv.movie.presentation.library.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import io.silv.core_ui.components.lazy.VerticalGridFastScroller
import io.silv.movie.data.lists.ContentItem
import io.silv.movie.data.lists.ContentList
import io.silv.movie.presentation.library.screenmodels.LibraryState
import kotlinx.collections.immutable.ImmutableList

@Composable
fun LibraryGridView(
    paddingValues: PaddingValues,
    state: LibraryState,
    onFavoritesClicked: () -> Unit,
    onListLongClick: (contentList: ContentList, items: ImmutableList<ContentItem>) -> Unit,
    onListClick: (contentList: ContentList) -> Unit,
    onPosterClick: (contentList: ContentList) -> Unit,
    modifier: Modifier,
) {
    val gridState = rememberLazyGridState()
    val cols = GridCells.Fixed(3)

    VerticalGridFastScroller(
        state = gridState,
        columns = cols,
        contentPadding = paddingValues,
        arrangement = Arrangement.SpaceEvenly
    ) {
        LazyVerticalGrid(
            columns = cols,
            state = gridState,
            contentPadding = paddingValues,
            modifier = modifier
        ) {
            item(key = "Library-Content") {
                ContentGridPreviewItem(
                    modifier = Modifier
                        .clickable { onFavoritesClicked() }
                        .padding(8.dp),
                    cover = {
                        ContentPreviewDefaults.LibraryContentPoster(
                            modifier = Modifier.fillMaxSize()
                        )
                    },
                    name = "Library Content",
                    count = state.favorites.size
                )
            }
            state.contentLists.fastForEach { (list, items) ->
                item(
                    key = list.id
                ) {
                    ContentGridPreviewItem(
                        modifier = Modifier
                            .combinedClickable(
                                onLongClick = { onListLongClick(list, items) },
                                onClick = { onListClick(list) }
                            )
                            .animateItemPlacement()
                            .padding(8.dp),
                        cover = {
                            ContentListPoster(
                                list = list,
                                items = items,
                                modifier =  Modifier
                                    .fillMaxSize()
                                    .clickable { onPosterClick(list) }
                            )
                        },
                        name = list.name,
                        count = items.size
                    )
                }
            }
        }
    }
}