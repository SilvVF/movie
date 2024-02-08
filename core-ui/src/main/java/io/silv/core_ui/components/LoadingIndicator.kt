package io.silv.core_ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems

@Composable
fun PageLoadingIndicator(
    modifier: Modifier = Modifier
) {
    LinearProgressIndicator(
        modifier = modifier.fillMaxWidth()
    )
}

fun LazyGridScope.loadingIndicatorItem(
    pagingItems: LazyPagingItems<*>
) {
    if (pagingItems.loadState.append is LoadState.Loading) {
        item(
            key = "loading-append",
            span = { GridItemSpan(maxLineSpan) }
        ) {
            PageLoadingIndicator()
        }
    }
}


