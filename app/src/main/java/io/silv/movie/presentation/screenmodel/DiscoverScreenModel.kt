package io.silv.movie.presentation.screenmodel

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
import io.silv.movie.data.content.lists.ContentItem
import io.silv.movie.data.content.lists.ContentList
import io.silv.movie.data.content.lists.ListWithPostersRpcResponse
import io.silv.movie.data.content.lists.ContentListRepository
import io.silv.movie.data.content.lists.toContentItem
import io.silv.movie.data.content.lists.toListPreviewItem
import io.silv.movie.data.content.movie.local.LocalContentDelegate
import io.silv.movie.data.prefrences.BasePreferences
import io.silv.movie.data.user.model.list.ListWithItems
import io.silv.movie.data.user.repository.ListRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
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
    val items: List<StateFlow<ContentItem>>,
)


class BrowseListsScreenModel(
    private val postgrest: Postgrest,
    private val contentListRepository: ContentListRepository,
    private val listRepository: ListRepository,
    private val local: LocalContentDelegate,
    basePreferences: BasePreferences,
    auth: Auth,
) : ScreenModel {

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
                }.onFailure { Timber.e(it) }
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
            }
                .onFailure { Timber.e(it) }
                .getOrDefault(emptyList())

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
        MutableStateFlow<List<Pair<ContentList, List<StateFlow<ContentItem>>>>?>(null)

    private val recentIds = basePreferences.recentlyViewedLists()

    val defaultLists = _defaultLists.asStateFlow()

    val subscribedRecommended = subscribedRecommendedResult.asStateFlow()
        .map { response ->
            response?.map { listWithPosters ->
                listWithPosters.toListPreviewItem(
                    contentListRepository,
                    local,
                    ioCoroutineScope
                )
            }
        }
        .stateIn(
            screenModelScope,
            SharingStarted.WhileSubscribed(5_000),
            null
        )

    val recentlyCreated = recentlyCreatedResult.asStateFlow()
        .map { response ->
            response?.map { listWithPosters ->
                listWithPosters.toListPreviewItem(contentListRepository, local, ioCoroutineScope)
            }
        }
        .stateIn(
            screenModelScope,
            SharingStarted.WhileSubscribed(5_000),
            null
        )

    val recentlyViewed = recentlyViewedResult.asStateFlow()
        .map { response ->
            response?.map { listWithPosters ->
                listWithPosters.toListPreviewItem(contentListRepository, local, ioCoroutineScope)
            }
        }
        .stateIn(
            screenModelScope,
            SharingStarted.WhileSubscribed(5_000),
            null
        )

    val popularLists = popularResult.asStateFlow()
        .map { response ->
            response?.map { listWithPosters ->
                listWithPosters.toListPreviewItem(contentListRepository, local, ioCoroutineScope)
            }
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
                    subscribedRecommendedResult.emit(emptyList())
                } else {
                    jobs.last().invoke()
                }
            }
            .launchIn(screenModelScope)
    }

    private var refreshJob: Job? = null

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
    ): Pair<ContentList, List<StateFlow<ContentItem>>> {
        val defList by lazy {
            ContentList(
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
                createdAt = listWithItems.createdAt.epochSeconds,
                inLibrary = false,
                subscribers = listWithItems.subscribers,
                pinned = false
            )
        }
        val list = contentListRepository.getListForSupabaseId(listWithItems.listId) ?: defList
        val posters = listWithItems.items.orEmpty().map { listItem ->
            val isMovie = listItem.movieId != -1L
            val id = listItem.movieId.takeIf { isMovie } ?: listItem.showId

            if (isMovie) {
                local.observeMoviePartialByIdOrNull(id).mapNotNull { it?.toContentItem() }
            } else {
                local.observeShowPartialByIdOrNull(id).mapNotNull { it?.toContentItem() }
            }
                .stateIn(
                    ioCoroutineScope, SharingStarted.WhileSubscribed(), ContentItem(
                        contentId = id,
                        isMovie = isMovie,
                        title = listItem.title,
                        posterUrl = listItem.posterPath,
                        favorite = false,
                        inLibraryLists = -1L,
                        posterLastUpdated = -1L,
                        lastModified = -1L,
                        description = "",
                        popularity = -1.0
                    )
                )
        }

        return list to posters
    }
}
