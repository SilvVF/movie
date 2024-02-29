package io.silv.movie.presentation.tv.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import io.silv.core_ui.components.CommonEntryItemDefaults
import io.silv.core_ui.components.EntryListItem
import io.silv.core_ui.components.PageLoadingIndicator
import io.silv.core_ui.util.isScrollingUp
import io.silv.core_ui.util.plus
import io.silv.movie.data.prefrences.PosterDisplayMode
import io.silv.movie.data.tv.TVShow
import io.silv.movie.presentation.LocalIsScrolling
import io.silv.movie.presentation.movie.browse.components.InLibraryBadge
import io.silv.movie.presentation.toPoster
import kotlinx.coroutines.flow.StateFlow

@Composable
fun BrowseTVSourceList(
    modifier: Modifier,
    pagingItems: LazyPagingItems<StateFlow<TVShow>>,
    contentPadding: PaddingValues,
    onShowClick: (TVShow) -> Unit,
    onShowLongClick: (TVShow) -> Unit,
) {
    val listState = rememberLazyListState()

    var isScrolling by LocalIsScrolling.current
    val isScrollingUp = listState.isScrollingUp()

    LaunchedEffect(isScrollingUp) {
        isScrolling = isScrollingUp
    }


    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = contentPadding + PaddingValues(vertical = 8.dp),
    ) {
        items(
            count = pagingItems.itemCount,
            contentType = pagingItems.itemContentType { PosterDisplayMode.List.hashCode() },
            key = pagingItems.itemKey { it.value.id }
        ) { index ->
            val tvShow by pagingItems[index]?.collectAsStateWithLifecycle() ?: return@items

            BrowseTVSourceListItem(
                show = tvShow,
                onClick = { onShowClick(tvShow) },
                onLongClick = { onShowLongClick(tvShow) },
            )
        }

        if (pagingItems.loadState.append is LoadState.Loading) {
            item(
                key = "loading-append"
            ) {
                PageLoadingIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun BrowseTVSourceListItem(
    show: TVShow,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    EntryListItem(
        title = show.title,
        coverData = show.toPoster(),
        coverAlpha = if (show.favorite) CommonEntryItemDefaults.BrowseFavoriteCoverAlpha else 1f,
        badge = {
            InLibraryBadge(enabled = show.favorite)
        },
        onLongClick = onLongClick,
        onClick = onClick,
    )
}