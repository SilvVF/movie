package io.silv.movie.presentation.screen

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import io.silv.core_ui.components.PullRefresh
import io.silv.core_ui.components.lazy.VerticalFastScroller
import io.silv.core_ui.util.copyToClipboard
import io.silv.core_ui.voyager.ContentScreen
import io.silv.movie.R
import io.silv.movie.coil.fetchers.model.UserProfileImageData
import io.silv.movie.data.content.lists.toContentItem
import io.silv.movie.data.content.movie.model.Credit
import io.silv.movie.koin4ScreenModel
import io.silv.movie.presentation.LocalContentInteractor
import io.silv.movie.presentation.LocalUser
import io.silv.movie.presentation.components.content.creditsPagingList
import io.silv.movie.presentation.components.content.movie.ExpandableDescription
import io.silv.movie.presentation.components.content.movie.MovieInfoBox
import io.silv.movie.presentation.components.dialog.CommentsBottomSheet
import io.silv.movie.presentation.covers.EditCoverAction
import io.silv.movie.presentation.covers.PosterCoverDialog
import io.silv.movie.presentation.covers.screenmodel.TVCoverScreenModel
import io.silv.movie.presentation.getActivityViewModel
import io.silv.movie.presentation.media.PlayerViewModel
import io.silv.movie.presentation.media.WatchContentActivity
import io.silv.movie.presentation.media.components.trailersList
import io.silv.movie.presentation.screenmodel.CommentsState
import io.silv.movie.presentation.screenmodel.ShowDetailsState
import io.silv.movie.presentation.screenmodel.TVViewScreenModel
import io.silv.movie.presentation.screenmodel.getCommentsScreenModel
import io.silv.movie.presentation.toPoster
import org.koin.core.parameter.parametersOf

data class TVViewScreen(
    override val id: Long,
    override val isMovie: Boolean = false,
): ContentScreen {

    override val key: ScreenKey
        get() = super.key + id + isMovie

    @Composable
    override fun Content() {
        val screenModel = koin4ScreenModel<TVViewScreenModel> { parametersOf(id) }
        val mainScreenModel by getActivityViewModel<PlayerViewModel>()
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val changeDialog = remember {
            { dialog: TVViewScreenModel.Dialog? -> screenModel.updateDialog(dialog) }
        }
        when (val state = screenModel.state.collectAsStateWithLifecycle().value) {
            ShowDetailsState.Error ->  Box(modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            ShowDetailsState.Loading -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }
            is ShowDetailsState.Success -> {
                val credits = screenModel.credits.collectAsLazyPagingItems()
                val commentsScreenModel = getCommentsScreenModel()

                val commentsState by commentsScreenModel.state.collectAsStateWithLifecycle()

                TVDetailsContent(
                    state = state,
                    commentsState = commentsState,
                    refresh = screenModel::refresh,
                    creditsProvider = { credits },
                    onPosterClick = { changeDialog(TVViewScreenModel.Dialog.FullCover) },
                    onVideoThumbnailClick = mainScreenModel::requestMediaQueue,
                    onViewCreditsClick = { navigator.push(CreditsViewScreen(state.show.id, false)) },
                    onCreditClick = { credit ->
                        credit.personId?.let {
                            navigator.push(
                                PersonViewScreen(
                                    it,
                                    credit.name,
                                    credit.profilePath
                                )
                            )
                        }
                    },
                    onWatchShowClick = {
                        context.startActivity(
                            Intent(context, WatchContentActivity::class.java)
                                .apply { putExtra("url", "https://vidsrc.to/embed/tv/${state.show.id}") }
                        )
                    },
                    onAddToList = { navigator.push(AddToListScreen(state.show.id, false)) },
                    onShowComments = { changeDialog(TVViewScreenModel.Dialog.Comments) }
                )
                val onDismissRequest =  { changeDialog(null) }
                when (state.dialog) {
                    null -> Unit
                    TVViewScreenModel.Dialog.FullCover -> {
                        val sm = koin4ScreenModel<TVCoverScreenModel> { parametersOf(state.show.id) }
                        val tvShow by sm.state.collectAsStateWithLifecycle()

                        if (tvShow != null) {
                            val getContent = rememberLauncherForActivityResult(
                                ActivityResultContracts.GetContent(),
                            ) {
                                if (it == null) return@rememberLauncherForActivityResult
                                sm.editCover(context, it)
                            }
                            val poster = remember(tvShow) { tvShow!!.toPoster() }
                            PosterCoverDialog(
                                coverDataProvider = { poster },
                                isCustomCover = remember(tvShow) { sm.hasCustomCover(tvShow!!) },
                                onShareClick = { sm.shareCover(context) },
                                onSaveClick = { sm.saveCover(context) },
                                snackbarHostState = sm.snackbarHostState,
                                onEditClick = if (tvShow!!.favorite || tvShow!!.inList) {
                                    {
                                        when (it) {
                                            EditCoverAction.EDIT -> getContent.launch("image/*")
                                            EditCoverAction.DELETE -> sm.deleteCustomCover(context)
                                        }
                                    }
                                } else null,
                                onDismissRequest = onDismissRequest,
                            )
                        }
                    }
                    TVViewScreenModel.Dialog.Comments -> {
                        CommentsBottomSheet(
                            onDismissRequest = onDismissRequest,
                            screenModel = commentsScreenModel
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TVDetailsContent(
    state: ShowDetailsState.Success,
    commentsState: CommentsState,
    refresh: () -> Unit,
    creditsProvider: () -> LazyPagingItems<Credit>,
    onPosterClick: () -> Unit,
    onViewCreditsClick: () -> Unit,
    onVideoThumbnailClick: (showId: Long, isMovie: Boolean, trailerId: String) -> Unit,
    onCreditClick: (credit: Credit) -> Unit,
    onWatchShowClick: () -> Unit,
    onAddToList: () -> Unit,
    onShowComments: () -> Unit,
) {
    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onWatchShowClick) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Filled.Tv, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Watch")
                }
            }
        }
    ) { paddingValues ->

        val topPadding = paddingValues.calculateTopPadding()
        val listState = rememberLazyListState()
        val contentInteractor = LocalContentInteractor.current

        PullRefresh(
            refreshing = state.refreshing,
            onRefresh = refresh,
            enabled = { true },
            indicatorPadding = PaddingValues(top = topPadding),
        ) {
            val layoutDirection = LocalLayoutDirection.current
            VerticalFastScroller(
                listState = listState,
                topContentPadding = topPadding,
                endContentPadding = paddingValues.calculateEndPadding(layoutDirection),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxHeight(),
                    state = listState,
                    contentPadding = PaddingValues(
                        start = paddingValues.calculateStartPadding(layoutDirection),
                        end = paddingValues.calculateEndPadding(layoutDirection),
                        bottom = paddingValues.calculateBottomPadding(),
                    ),
                ) {
                    item(
                        key = "Info-Box",
                        contentType = "Info-Box",
                    ) {
                        MovieInfoBox(
                            isTabletUi = false,
                            appBarPadding = topPadding,
                            title = state.show.title,
                            author = remember (state.show.productionCompanies) {
                                state.show.productionCompanies?.joinToString()
                            },
                            artist = "",
                            sourceName = stringResource(id = R.string.tmdb),
                            isStubSource = false,
                            coverDataProvider = { state.show.toPoster() },
                            status = state.show.status,
                            onCoverClick = onPosterClick,
                            doSearch = { _, _ -> },
                        )
                    }
                    item(
                        key = "movie-actions"
                    ) {
                        ContentActionRow(
                            favorite = state.show.favorite,
                            onAddToLibraryClicked = { contentInteractor.toggleFavorite(state.show.toContentItem()) },
                            onAddToList = onAddToList,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item("Description-Tags") {
                        val context = LocalContext.current
                        ExpandableDescription(
                            defaultExpandState = false,
                            description = state.show.overview,
                            tagsProvider = { state.show.genres },
                            onTagSearch = {},
                            onCopyTagToClipboard = {
                                context.copyToClipboard("tag", it)
                            }
                        )
                    }
                    item {
                        CommentsPreview(
                            count = commentsState.messageCount?.toInt() ?: 0,
                            profileImageData =  commentsState.recentMessage?.let {
                                UserProfileImageData(
                                    userId = it.userId.orEmpty(),
                                    isUserMe = it.userId == LocalUser.current?.userId,
                                    path = it.users?.profileImage
                                )
                            },
                            username = commentsState.recentMessage?.users?.username,
                            lastMessage = commentsState.recentMessage?.message,
                            modifier = Modifier
                                .padding(16.dp)
                                .clickable {
                                    onShowComments()
                                }
                        )
                    }
                    creditsPagingList(
                        creditsProvider = creditsProvider,
                        onCreditClick = onCreditClick,
                        onCreditLongClick = {},
                        onViewClick = onViewCreditsClick
                    )
                    trailersList(
                        trailers = state.trailers,
                        onClick = {
                            onVideoThumbnailClick(state.show.id, false, it.id)
                        },
                        onYoutubeClick = {}
                    )
                }
            }
        }
    }
}
