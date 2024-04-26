package io.silv.movie.presentation.browse.lists

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.rpc
import io.silv.core_ui.voyager.ioCoroutineScope
import io.silv.movie.data.lists.ContentItem
import io.silv.movie.data.lists.ContentList
import io.silv.movie.data.lists.ContentListRepository
import io.silv.movie.data.lists.toContentItem
import io.silv.movie.data.movie.interactor.GetMovie
import io.silv.movie.data.movie.interactor.GetRemoteMovie
import io.silv.movie.data.prefrences.BasePreferences
import io.silv.movie.data.tv.interactor.GetRemoteTVShows
import io.silv.movie.data.tv.interactor.GetShow
import io.silv.movie.data.user.ListRepository
import io.silv.movie.data.user.ListWithItems
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import timber.log.Timber

@Serializable
data class PopularListParams(
    val lim: Int,
    val off: Int,
)

@Serializable
data class ListByIdParams(
    @SerialName("list_ids")
    val listIds: String
) {

    companion object {
        fun of(list: List<String>): ListByIdParams {
            return ListByIdParams(
                listIds = "{${list.joinToString()}}"
            )
        }
    }
}


data class ListPreviewItem(
    val list: ContentList,
    val username: String,
    val profileImage: String?,
    val items: ImmutableList<StateFlow<ContentItem?>>,
)


class BrowseListsScreenModel(
    private val postgrest: Postgrest,
    private val contentListRepository: ContentListRepository,
    private val getMovie: GetMovie,
    private val getShow: GetShow,
    private val getRemoteMovie: GetRemoteMovie,
    private val getRemoteTVShows: GetRemoteTVShows,
    private val listRepository: ListRepository,
    basePreferences: BasePreferences,
    auth: Auth,
): ScreenModel {

    private val DEFAULT_LIST_USER = "c532e5da-71ca-4b4b-b896-d1d36f335149"
    var refreshing by mutableStateOf(false)
        private set

    private val jobs = listOf(
        suspend {
            popularResult.emit(
                runCatching {
                    postgrest.rpc(
                        "select_most_popular_lists_with_poster_items",
                        PopularListParams(10, 0)
                    )
                        .decodeList<ListWithPostersRpcResponse>()
                }
                    .onFailure { Timber.e(it) }
                    .getOrDefault(emptyList())
            )
        },
        suspend {
            recentlyCreatedResult.emit(
                runCatching {
                    postgrest.rpc(
                        "select_most_recent_lists_with_poster_items",
                        PopularListParams(10, 0)
                    )
                        .decodeList<ListWithPostersRpcResponse>()
                } .onFailure { Timber.e(it) }
                    .getOrDefault(emptyList())
            )
        },
        suspend {
            recentlyViewedResult.emit(
                runCatching {
                    postgrest.rpc(
                        "select_lists_by_ids_with_poster",
                        ListByIdParams.of(
                            recentIds.get().takeLast(8).map { it.second }
                        )
                    )
                        .decodeList<ListWithPostersRpcResponse>()
                }
                    .onFailure { Timber.e(it) }
                    .getOrDefault(emptyList())
            )
        },
        suspend {
            val lists = runCatching {
                listRepository
                    .selectListsByUserId(DEFAULT_LIST_USER)
                    ?.map { listWithItems ->
                        toContentListWithItems(listWithItems)
                    }
                    .orEmpty()
                    .toImmutableList()
            }
                .onFailure { Timber.e(it) }
                .getOrDefault(persistentListOf())

            _defaultLists.emit(lists)
        },
        suspend sub@{
            val fromSubscribed =
                listRepository.selectRecommendedFromSubscriptions() ?: return@sub
            subscribedRecommendedResult.emit(fromSubscribed)
        }
    )

    private val popularResult =
        MutableStateFlow<List<ListWithPostersRpcResponse>?>(null)
    private val recentlyCreatedResult =
        MutableStateFlow<List<ListWithPostersRpcResponse>?>(null)
    private val recentlyViewedResult =
        MutableStateFlow<List<ListWithPostersRpcResponse>?>(null)
    private val subscribedRecommendedResult =
        MutableStateFlow<List<ListWithPostersRpcResponse>?>(null)
    private val _defaultLists =
        MutableStateFlow<ImmutableList<Pair<ContentList, ImmutableList<StateFlow<ContentItem>>>>?>(null)

    private val recentIds = basePreferences.recentlyViewedLists()

    val defaultLists = _defaultLists.asStateFlow()

    val subscribedRecommended = subscribedRecommendedResult.asStateFlow()
        .map { response ->
            response?.map { listWithPosters ->
                listWithPosters.toListPreviewItem(contentListRepository, getShow, getMovie, ioCoroutineScope)
            }
                ?.toImmutableList()
        }
        .stateIn(
            screenModelScope,
            SharingStarted.WhileSubscribed(5_000),
            null
        )

    val recentlyCreated= recentlyCreatedResult.asStateFlow()
        .map { response ->
            response?.map { listWithPosters ->
                listWithPosters.toListPreviewItem(contentListRepository, getShow, getMovie, ioCoroutineScope)
            }
                ?.toImmutableList()
        }
        .stateIn(
            screenModelScope,
            SharingStarted.WhileSubscribed(5_000),
            null
        )

    val recentlyViewed= recentlyViewedResult.asStateFlow()
        .map { response ->
            response?.map { listWithPosters ->
                listWithPosters.toListPreviewItem(contentListRepository, getShow, getMovie, ioCoroutineScope)
            }
                ?.toImmutableList()
        }
        .stateIn(
            screenModelScope,
            SharingStarted.WhileSubscribed(5_000),
            null
        )

    val popularLists = popularResult.asStateFlow()
        .map { response ->
            response?.map { listWithPosters ->
                listWithPosters.toListPreviewItem(contentListRepository, getShow, getMovie, ioCoroutineScope)
            }
                ?.toImmutableList()
        }
        .stateIn(
            screenModelScope,
            SharingStarted.WhileSubscribed(5_000),
            null
        )

    init {
        refresh()

        basePreferences.recentlyViewedLists()
            .changes()
            .drop(1)
            .onEach {
                jobs[2].invoke()
            }
            .launchIn(screenModelScope)

        auth.sessionStatus
            .drop(1)
            .onEach {
                if (it !is SessionStatus.Authenticated) {
                    subscribedRecommendedResult.emit(persistentListOf())
                } else {
                    jobs.last().invoke()
                }
            }
            .launchIn(screenModelScope)
    }

    private var refreshJob: Job?  = null

    fun refresh(isUserAction: Boolean = false) {
        if (refreshJob?.isActive == true)
            return

        if (isUserAction) {
            refreshing = true
        }

        refreshJob = ioCoroutineScope.launch {
            supervisorScope {
                val running = jobs.map {
                    launch {
                        runCatching { it() }.onFailure { Timber.e(it) }
                    }
                }

                running.joinAll()

                withContext(Dispatchers.Main) { refreshing = false }
            }
        }
    }

    private suspend fun toContentListWithItems(
        listWithItems: ListWithItems
    ): Pair<ContentList, ImmutableList<StateFlow<ContentItem>>> {
        val local = contentListRepository.getListForSupabaseId(listWithItems.listId)
        val list = local ?: ContentList(
            id = -1,
            supabaseId = listWithItems.listId,
            createdBy = "c532e5da-71ca-4b4b-b896-d1d36f335149",
            lastSynced = null,
            public = true,
            name = listWithItems.name,
            username = "Default User",
            description = listWithItems.description,
            lastModified = -1L,
            posterLastModified = -1L,
            createdAt = listWithItems.createdAt.toEpochMilliseconds(),
            inLibrary = false,
            subscribers = listWithItems.subscribers
        )
        val posters = listWithItems.items.orEmpty().map {
            val isMovie = it.movieId != -1L
            val id = it.movieId.takeIf { isMovie } ?: it.showId

            val defItem by lazy {
                ContentItem(
                    contentId = id,
                    isMovie = isMovie,
                    title = it.title,
                    posterUrl = "https://image.tmdb.org/t/p/original/${it.posterPath}",
                    favorite = false,
                    inLibraryLists = -1L,
                    posterLastUpdated = -1L,
                    lastModified = -1L,
                    description = "",
                    popularity = -1.0
                )
            }

            val item = if(isMovie)
                getMovie.subscribePartialOrNull(id).map { it?.toContentItem() ?: defItem }
            else
                getShow.subscribePartialOrNull(id).map { it?.toContentItem() ?: defItem }

            item.stateIn(ioCoroutineScope)
        }
        return Pair(list, posters.toImmutableList())
    }
}
