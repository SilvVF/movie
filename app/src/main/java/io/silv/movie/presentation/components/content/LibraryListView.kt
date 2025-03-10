package io.silv.movie.presentation.components.content

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import io.silv.core_ui.components.lazy.VerticalFastScroller
import io.silv.movie.R
import io.silv.movie.data.model.ContentItem
import io.silv.movie.data.model.ContentList
import io.silv.movie.presentation.screenmodel.LibraryState
import io.silv.movie.presentation.tabs.SharedElement
import io.silv.movie.presentation.tabs.registerSharedElement

@Composable
fun LibraryListView(
    paddingValues: PaddingValues,
    state: LibraryState,
    onFavoritesClicked: () -> Unit,
    onListLongClick: (contentList: ContentList, items: List<ContentItem>) -> Unit,
    onListClick: (contentList: ContentList) -> Unit,
    onPosterClick: (contentList: ContentList) -> Unit,
    modifier: Modifier = Modifier,
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
                    modifier = Modifier
                        .clickable { onFavoritesClicked() }
                        .padding(8.dp),
                    textModifier = Modifier
                        .fillMaxWidth()
                        .registerSharedElement(SharedElement.From("${SharedElement.PREFIX_LIST_NAME}-1")),
                    cover = {
                        ContentPreviewDefaults.LibraryContentPoster(
                            Modifier
                                .aspectRatio(1f)
                                .registerSharedElement(SharedElement.List(-1))
                        )
                    },
                    pinned = true,
                    name = stringResource(id = R.string.favorites_top_bar_title),
                    description = stringResource(R.string.content_preview_items , state.favorites.size)
                )
            }
            state.contentLists.fastForEach { (list, items) ->
                item(
                    key = list.id
                ) {
                    ContentListPreview(
                        modifier = Modifier
                            .combinedClickable(
                                onLongClick = { onListLongClick(list, items) },
                                onClick = { onListClick(list) }
                            )
                            .animateItem()
                            .padding(8.dp),
                        pinned = list.pinned,
                        cover = {
                            ContentListPoster(
                                list = list,
                                items = items,
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clickable { onPosterClick(list) }
                                    .registerSharedElement(SharedElement.List(list.id))
                            )
                        },
                        textModifier = Modifier
                            .fillMaxWidth()
                            .registerSharedElement(SharedElement.From("${SharedElement.PREFIX_LIST_NAME}${list.id}")),
                        name = list.name,
                        description = list.description.ifEmpty {
                            when {
                                items.isEmpty() ->
                                    stringResource(id = R.string.content_preview_no_items)
                                else -> stringResource(R.string.content_preview_items, items.size)
                            }
                        }
                    )
                }
            }
        }
    }
}


