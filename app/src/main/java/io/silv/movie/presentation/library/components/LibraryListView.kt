package io.silv.movie.presentation.library.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import io.silv.core_ui.components.VerticalFastScroller
import io.silv.movie.data.lists.ContentListItem
import io.silv.movie.presentation.library.LibraryState

@Composable
fun LibraryListView(
    paddingValues: PaddingValues,
    state: LibraryState,
    modifier: Modifier,
) {
    val topPadding = paddingValues.calculateTopPadding()
    val listState = rememberLazyListState()
    val layoutDirection = LocalLayoutDirection.current

    VerticalFastScroller(
        listState = listState,
        topContentPadding = topPadding,
        endContentPadding = paddingValues.calculateEndPadding(layoutDirection),
        bottomContentPadding = paddingValues.calculateBottomPadding(),
    ) {
        LazyColumn(
            modifier = modifier
                .fillMaxSize(),
            state = listState,
            contentPadding = paddingValues,
        ) {
            item(key = "Library-Content") {
                ContentListPreview(
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
                    ContentListPreview(
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