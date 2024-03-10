package io.silv.movie.presentation.library.components

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
import io.silv.core_ui.components.VerticalGridFastScroller
import io.silv.movie.data.lists.ContentListItem
import io.silv.movie.presentation.library.LibraryState

@Composable
fun LibraryGridView(
    paddingValues: PaddingValues,
    state: LibraryState,
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
                    modifier = Modifier.padding(8.dp),
                    cover = {
                        ContentPreviewDefaults.LibraryContentPoster()
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
                            .animateItemPlacement()
                            .padding(8.dp),
                        cover = {
                            if (items.size < 4) {
                                ContentPreviewDefaults.SingleItemPoster(
                                    modifier = Modifier.fillMaxSize(),
                                    item = items.first()
                                )
                            } else {
                                ContentPreviewDefaults.MultiItemPoster(
                                    modifier = Modifier.fillMaxSize(),
                                    items = items
                                )
                            }
                        },
                        name = list.name,
                        count = when (items.first()) {
                            is ContentListItem.PlaceHolder -> 0
                            is ContentListItem.Item -> items.size
                        }
                    )
                }
            }
        }
    }
}