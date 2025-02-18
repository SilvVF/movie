package io.silv.movie.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import androidx.paging.map
import io.github.jan.supabase.auth.Auth
import io.silv.movie.core.SMovie
import io.silv.movie.core.SShow
import io.silv.movie.core.suspendRunCatching
import io.silv.movie.data.local.ContentListRepository
import io.silv.movie.data.local.LocalContentDelegate
import io.silv.movie.data.local.networkToLocalMovie
import io.silv.movie.data.local.networkToLocalShow
import io.silv.movie.data.model.ContentItem
import io.silv.movie.data.model.ContentList
import io.silv.movie.data.model.ContentPagedType
import io.silv.movie.data.model.toContentItem
import io.silv.movie.data.model.toDomain
import io.silv.movie.data.model.toUpdate
import io.silv.movie.data.network.NetworkContentDelegate
import io.silv.movie.data.network.getMoviePager
import io.silv.movie.data.network.getShowPager
import io.silv.movie.data.supabase.BackendRepository
import io.silv.movie.data.supabase.ContentType
import io.silv.movie.data.supabase.ListRepository
import io.silv.movie.data.supabase.model.list.toUserListUpdate
import io.silv.movie.presentation.covers.cache.MovieCoverCache
import io.silv.movie.presentation.covers.cache.TVShowCoverCache
import io.silv.movie.presentation.screenmodel.uniqueBy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.supervisorScope

fun interface EditContentList {
    suspend operator fun invoke(
        list: ContentList,
        update: (prev: ContentList) -> ContentList
    ): Result<ContentList>

    companion object {
        fun create(
            network: ListRepository,
            local: ContentListRepository,
            auth: Auth,
        ) = EditContentList { list, update ->
            suspendRunCatching {
                val new = update(list)

                if (new.supabaseId != null && auth.currentUserOrNull()!!.id == list.createdBy) {
                    val result = network.updateList(new.toUserListUpdate())
                    if (!result) {
                        error("Failed to remove from network")
                    }
                }
                local.updateList(new.toUpdate())

                new
            }
        }
    }
}



fun interface GetContentPagerFlow {

    operator fun invoke(
        contentType: ContentType,
        contentPagedType: ContentPagedType,
        cachedIn: CoroutineScope,
        transform: PagingData<StateFlow<ContentItem>>.() -> PagingData<StateFlow<ContentItem>>
    ): Flow<PagingData<StateFlow<ContentItem>>>

    companion object {
        fun create(
            local: LocalContentDelegate,
            network: NetworkContentDelegate
        ): GetContentPagerFlow =
            GetContentPagerFlow { contentType, contentPagedType, cachedIn, transform ->
                Pager(PagingConfig(pageSize = 25)) {
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
}

fun interface DeleteContentList {

    suspend operator fun invoke(
        list: ContentList,
        movieCoverCache: MovieCoverCache,
        showCoverCache: TVShowCoverCache,
    ): Result<Unit>

    companion object {
        fun create(
            network: ListRepository,
            local: LocalContentDelegate,
            listRepo: ContentListRepository,
            backendRepository: BackendRepository,
        ): DeleteContentList = DeleteContentList { list, movieCoverCache, showCoverCache ->
            suspendRunCatching {
                if (list.supabaseId != null && backendRepository.currentUser.value?.userId == list.createdBy) {
                    val result = network.deleteList(list.supabaseId)

                    if (!result) {
                        error("Failed to remove list from network")
                    }
                }
                supervisorScope {
                    if (list.inLibrary) {
                        for (item in listRepo.getListItems(list.id)) {
                            if (item.inLibraryLists == 1L && !item.favorite) {
                                if (item.isMovie) {
                                    val movie = local.getMovieById(item.contentId) ?: continue
                                    movieCoverCache.deleteFromCache(movie)
                                } else {
                                    val show = local.getShowById(item.contentId) ?: continue
                                    showCoverCache.deleteFromCache(show)
                                }
                            }
                        }
                    }
                }
                listRepo.deleteList(list)
            }
        }
    }
}