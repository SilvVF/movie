package io.silv.movie.presentation.library.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.core.net.toUri
import io.silv.core_ui.components.lazy.VerticalFastScroller
import io.silv.movie.R
import io.silv.movie.data.cache.ListCoverCache
import io.silv.movie.data.lists.ContentList
import io.silv.movie.data.lists.ContentListItem
import io.silv.movie.presentation.library.browse.LibraryState
import org.koin.compose.koinInject

@Composable
fun LibraryListView(
    paddingValues: PaddingValues,
    state: LibraryState,
    onFavoritesClicked: () -> Unit,
    onListLongClick: (contentList: ContentList) -> Unit,
    onListClick: (contentList: ContentList) -> Unit,
    onPosterClick: (contentList: ContentList) -> Unit,
    modifier: Modifier = Modifier,
) {
    val topPadding = paddingValues.calculateTopPadding()
    val listState = rememberLazyListState()
    val layoutDirection = LocalLayoutDirection.current
    val cache = koinInject<ListCoverCache>()

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
                    cover = {
                        ContentPreviewDefaults.LibraryContentPoster(Modifier.fillMaxSize())
                    },
                    name = stringResource(id = R.string.library_content_name),
                    count = state.favorites.size
                )
            }
            state.contentLists.fastForEach { (list, items) ->
                item(
                    key = list.id
                ) {
                    ContentListPreview(
                        modifier = Modifier
                            .combinedClickable(
                                onLongClick = { onListLongClick(list) },
                                onClick = { onListClick(list) }
                            )
                            .animateItemPlacement()
                            .padding(8.dp),
                        cover = {
                            val file by remember("${list.id};${list.posterLastModified}") {
                                derivedStateOf {
                                    cache.getCustomCoverFile(list.id)
                                }
                            }
                            val fileExists by remember("${list.id};${list.posterLastModified}") {
                                derivedStateOf { file.exists() }
                            }

                            if (fileExists) {
                                ContentPreviewDefaults.CustomListPoster(
                                    modifier = Modifier.fillMaxSize()
                                        .clickable{ onPosterClick(list) },
                                    uri = file.toUri()
                                )
                            } else {
                                if (items.size < 4) {
                                    ContentPreviewDefaults.SingleItemPoster(
                                        modifier = Modifier.fillMaxSize()
                                            .clickable{ onPosterClick(list) },
                                        item = items.first()
                                    )
                                } else {
                                    ContentPreviewDefaults.MultiItemPosterContentLIst(
                                        modifier = Modifier.fillMaxSize()
                                            .clickable{ onPosterClick(list) },
                                        content = items
                                    )
                                }
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