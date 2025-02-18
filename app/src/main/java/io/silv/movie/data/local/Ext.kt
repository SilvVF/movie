package io.silv.movie.data.local

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import androidx.paging.map
import io.silv.movie.core.SMovie
import io.silv.movie.core.SShow
import io.silv.movie.data.model.ContentItem
import io.silv.movie.data.model.ContentPagedType
import io.silv.movie.data.model.toContentItem
import io.silv.movie.data.model.toDomain
import io.silv.movie.data.network.NetworkContentDelegate
import io.silv.movie.data.network.getMoviePager
import io.silv.movie.data.network.getShowPager
import io.silv.movie.data.supabase.ContentType
import io.silv.movie.presentation.screenmodel.uniqueBy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn

fun interface GetContentPagerFlow {
    operator fun invoke(
        contentType: ContentType,
        contentPagedType: ContentPagedType,
        cachedIn: CoroutineScope,
        transform: PagingData<StateFlow<ContentItem>>.() -> PagingData<StateFlow<ContentItem>>
    ): Flow<PagingData<StateFlow<ContentItem>>>

    companion object {
        fun create(local: LocalContentDelegate, network: NetworkContentDelegate): GetContentPagerFlow =
            GetContentPagerImpl(local, network)
    }
}

private class GetContentPagerImpl(
    private val local: LocalContentDelegate,
    private val network: NetworkContentDelegate
) : GetContentPagerFlow {


    override fun invoke(
        contentType: ContentType,
        contentPagedType: ContentPagedType,
        cachedIn: CoroutineScope,
        transform: PagingData<StateFlow<ContentItem>>.() -> PagingData<StateFlow<ContentItem>>,
    ): Flow<PagingData<StateFlow<ContentItem>>> {
        return Pager(PagingConfig(pageSize = 25)) {
            if (contentType == ContentType.Movie) {
                network.getMoviePager(contentPagedType)
            } else {
                network.getShowPager(contentPagedType)
            }
        }.flow.map { pagingData ->
            pagingData.map { item ->
                when (item) {
                    is SMovie -> {
                        val inserted = local.networkToLocalMovie(item.toDomain())
                        local.observeMoviePartialById(inserted.id)
                            .mapNotNull { movie -> movie?.toContentItem() }
                            .stateIn(cachedIn)
                    }

                    is SShow -> {
                        val inserted = local.networkToLocalShow(item.toDomain())
                        local.observeShowPartialById(inserted.id)
                            .mapNotNull { show -> show?.toContentItem() }
                            .stateIn(cachedIn)
                    }

                    else -> error("invalid type passed to get content pager")
                }
            }
                .filter { it.value.posterUrl.isNullOrBlank().not() }
                .uniqueBy { it.value.contentId }
                .transform()
        }
            .cachedIn(cachedIn)
    }
}