package io.silv.movie.presentation.browse.lists

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.rpc
import io.silv.core_ui.util.rememberDominantColor
import io.silv.movie.LocalUser
import io.silv.movie.data.lists.ContentItem
import io.silv.movie.data.lists.ContentList
import io.silv.movie.data.lists.ContentListRepository
import io.silv.movie.data.movie.interactor.GetMovie
import io.silv.movie.data.prefrences.BasePreferences
import io.silv.movie.data.tv.interactor.GetShow
import io.silv.movie.presentation.library.components.ContentListPosterItems
import io.silv.movie.presentation.library.screens.ListViewScreen
import io.silv.movie.presentation.profile.UserProfileImage
import io.silv.movie.rememberProfileImageData
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
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
    val items: ImmutableList<ContentItem>,
)

class BrowseListsScreenModel(
    private val postgrest: Postgrest,
    private val contentListRepository: ContentListRepository,
    private val getMovie: GetMovie,
    private val getShow: GetShow,
    private val basePreferences: BasePreferences,
): ScreenModel {

    private val popularResult = MutableStateFlow<List<ListWithPostersRpcResponse>>(emptyList())
    private val recentResult = MutableStateFlow<List<ListWithPostersRpcResponse>>(emptyList())

    private val recentIds = basePreferences.recentlyViewedLists()

    val recentlyViewed = recentResult.asStateFlow()
        .map { response ->
            response.map { listWithPosters ->
                listWithPosters.toListPreviewItem(contentListRepository, getShow, getMovie)
            }
        }
        .stateIn(
            screenModelScope,
            SharingStarted.WhileSubscribed(5_000),
            persistentListOf()
        )

    val popularLists = popularResult.asStateFlow()
        .map { response ->
            response.map { listWithPosters ->
                listWithPosters.toListPreviewItem(contentListRepository, getShow, getMovie)
            }
        }
        .stateIn(
            screenModelScope,
            SharingStarted.WhileSubscribed(5_000),
            persistentListOf()
        )

    init {
        refresh()
    }

    private var refreshJob: Job?  = null

    private fun refresh() {
        if (refreshJob?.isActive == true)
            return

        refreshJob = screenModelScope.launch {
            try {
                supervisorScope {
                    launch {
                        popularResult.emit(
                            postgrest.rpc(
                                "select_most_popular_lists_with_poster_items",
                                PopularListParams(10, 0)
                            )
                                .decodeList<ListWithPostersRpcResponse>()
                        )
                            .also { Timber.d(it.toString()) }
                    }
                    launch {
                        recentResult.emit(
                            postgrest.rpc(
                                "select_lists_by_ids_with_poster",
                                ListByIdParams.of(
                                    recentIds.get().toList()
                                )
                            )
                                .decodeList<ListWithPostersRpcResponse>()
                        )
                            .also { Timber.d(it.toString()) }
                    }
                }
            } catch (e :Exception) {
                Timber.e(e)
            }
        }
    }
}


object BrowseListsScreen: Screen {

    @Composable
    override fun Content(){

        val screenModel = getScreenModel<BrowseListsScreenModel>()
        val lists by screenModel.popularLists.collectAsStateWithLifecycle()
        val recentlyViewed by screenModel.recentlyViewed.collectAsStateWithLifecycle()
        val navigator = LocalNavigator.currentOrThrow
        val user = LocalUser.current
        val profileImageData = user.rememberProfileImageData()


        val dominantColor by rememberDominantColor(data = profileImageData)

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        if (user != null) {
                            UserProfileImage(
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .size(36.dp)
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                dominantColor,
                                                Color.Transparent
                                            )
                                        )
                                    )
                                    .padding(4.dp)
                                    .clip(CircleShape)
                                    .aspectRatio(1f),
                                contentDescription = user.username
                            )
                        }
                    },
                    title = {
                        user?.let { Text(it.username, style = MaterialTheme.typography.labelLarge) }
                    },
                    actions = {
                        IconButton(onClick = { /*TODO*/ }) {
                            Icon(imageVector = Icons.Default.Search, contentDescription = null)
                        }
                    }
                )
            }
        ) { paddingValues ->
            LazyColumn(
                contentPadding = paddingValues,
                modifier = Modifier.fillMaxSize(),
            ) {
                item {
                    FlowRow(
                        maxItemsInEachRow = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .wrapContentHeight()
                    ) {
                        recentlyViewed.forEachIndexed { i, it ->
                            RecentlyViewedPreview(
                                modifier = Modifier
                                    .padding(vertical = 4.dp,)
                                    .padding(
                                        start = if (i % 2 == 0) 0.dp else 4.dp,
                                        end = if (i % 2 == 0) 4.dp else 0.dp
                                    )
                                    .clickable {
                                        navigator.push(
                                            ListViewScreen(
                                                it.list.id,
                                                it.list.supabaseId.orEmpty()
                                            )
                                        )
                                    }
                                    .weight(1f)
                                    .height(64.dp)
                                    .clip(MaterialTheme.shapes.extraSmall)
                                    .background(Color.DarkGray),
                                cover = {
                                    ContentListPosterItems(
                                        list = it.list,
                                        items = it.items,
                                    )
                                },
                                name = it.list.name,
                            )
                        }
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Most popular",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .padding(vertical = 22.dp)
                        )
                        TextButton(
                            onClick = { }
                        ) {
                            Text(text = "View more")
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                item {
                    LazyRow {
                        item { Spacer(modifier = Modifier.width(12.dp)) }
                        items(
                            items = lists, key = { it.list.supabaseId.orEmpty() + it.list.id }
                        ) {
                            RowPreviewItem(
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .clickable {
                                        navigator.push(
                                            ListViewScreen(
                                                it.list.id,
                                                it.list.supabaseId.orEmpty()
                                            )
                                        )
                                    },
                                cover = {
                                    ContentListPosterItems(
                                        list = it.list,
                                        items = it.items,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(120.dp)
                                    )
                                },
                                name = it.list.name,
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun RowPreviewItem(
    cover: @Composable () -> Unit,
    name: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight(0.8f)
                .fillMaxWidth()
        ) {
            cover()
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = name,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            modifier = Modifier.padding(top = 2.dp),
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
fun RecentlyViewedPreview(
    cover: @Composable () -> Unit,
    name: String,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier.fillMaxHeight()
        ) {
            cover()
        }
        Spacer(Modifier.width(12.dp))
        Column(
            modifier = Modifier
                .weight(1f, true)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = name,
                maxLines = 2,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
