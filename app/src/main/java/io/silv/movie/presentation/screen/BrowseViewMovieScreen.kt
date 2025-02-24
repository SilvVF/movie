package io.silv.movie.presentation.screen

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import io.silv.core_ui.components.PullRefresh
import io.silv.core_ui.components.lazy.VerticalFastScroller
import io.silv.core_ui.util.copyToClipboard
import io.silv.core_ui.voyager.ContentScreen
import io.silv.movie.R
import io.silv.movie.coil.fetchers.model.UserProfileImageData
import io.silv.movie.data.model.Credit
import io.silv.movie.data.model.toContentItem
import io.silv.movie.presentation.LocalContentInteractor
import io.silv.movie.presentation.LocalUser
import io.silv.movie.presentation.components.content.creditsPagingList
import io.silv.movie.presentation.components.content.movie.ExpandableDescription
import io.silv.movie.presentation.components.content.movie.MovieInfoBox
import io.silv.movie.presentation.components.dialog.CommentsBottomSheet
import io.silv.movie.presentation.components.profile.UserProfileImage
import io.silv.movie.presentation.media.PlayerPresenter
import io.silv.movie.presentation.media.WatchContentActivity
import io.silv.movie.presentation.media.components.trailersList
import io.silv.movie.presentation.screenmodel.CommentsState
import io.silv.movie.presentation.screenmodel.MovieDetailsState
import io.silv.movie.presentation.screenmodel.MovieViewScreenModel
import io.silv.movie.presentation.screenmodel.getCommentsScreenModel
import io.silv.movie.presentation.toPoster
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

data class MovieViewScreen(
    override val id: Long,
): ContentScreen {

    override val isMovie: Boolean
        get() = true

    override val key: ScreenKey
        get() = super.key + id + isMovie

    @Composable
    override fun Content() {
        val screenModel = koinScreenModel<MovieViewScreenModel> { parametersOf(id) }
        val playerPresenter = koinInject<PlayerPresenter>()
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val changeDialog = remember {
            { dialog: MovieViewScreenModel.Dialog? -> screenModel.updateDialog(dialog) }
        }
        val commentsScreenModel = getCommentsScreenModel()
        val commentsState by commentsScreenModel.state.collectAsStateWithLifecycle()

        when (val state = screenModel.state.collectAsStateWithLifecycle().value) {
            MovieDetailsState.Error -> Box(modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            MovieDetailsState.Loading -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }

            is MovieDetailsState.Success -> {
                val credits = screenModel.credits.collectAsLazyPagingItems()
                MovieDetailsContent(
                        state = state,
                        refresh = screenModel::refresh,
                        creditsProvider = { credits },
                        onPosterClick = {
                            changeDialog(MovieViewScreenModel.Dialog.FullCover)
                        },
                        onVideoThumbnailClick = playerPresenter::requestMediaQueue,
                        onViewCreditsClick = {
                            navigator.push(
                                CreditsViewScreen(
                                    state.movie.id,
                                    true
                                )
                            )
                        },
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
                        onWatchMovieClick = {
                            context.startActivity(
                                Intent(context, WatchContentActivity::class.java)
                                    .apply {
                                        putExtra(
                                            "url",
                                            "https://vidsrc.to/embed/movie/${state.movie.id}"
                                        )
                                    }
                            )
                        },
                        onAddToList = { navigator.push(AddToListScreen(state.movie.id, true)) },
                        onShowComments = {
                            changeDialog(MovieViewScreenModel.Dialog.Comments)
                        },
                        commentsState = commentsState
                    )
                val onDismissRequest = { changeDialog(null) }
                when (state.dialog) {
                    null -> Unit
                    MovieViewScreenModel.Dialog.FullCover -> {
                       ChangeMovieCoverDialog(state.movie.id, onDismissRequest = onDismissRequest)
                    }

                    MovieViewScreenModel.Dialog.Comments -> CommentsBottomSheet(
                        onDismissRequest = onDismissRequest,
                        screenModel = commentsScreenModel
                    )
                }
            }
        }
    }
}

@Composable
private fun MovieDetailsContent(
    state: MovieDetailsState.Success,
    commentsState: CommentsState,
    creditsProvider: () -> LazyPagingItems<Credit>,
    refresh: () -> Unit,
    onPosterClick: () -> Unit,
    onViewCreditsClick: () -> Unit,
    onVideoThumbnailClick: (movieId: Long, isMovie: Boolean,  trailerId: String) -> Unit,
    onCreditClick: (credit: Credit) -> Unit,
    onWatchMovieClick: () -> Unit,
    onAddToList: () -> Unit,
    onShowComments: () -> Unit,
) {
    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onWatchMovieClick) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Filled.Movie, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Watch")
                }
            }
        }
    ) { paddingValues ->

        val topPadding = paddingValues.calculateTopPadding()
        val listState = rememberLazyListState()
        val hazeState = remember { HazeState() }
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
                bottomContentPadding = paddingValues.calculateBottomPadding(),
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .haze(
                            state = hazeState,
                            style = HazeDefaults.style(MaterialTheme.colorScheme.background),
                        ),
                    state = listState,
                    contentPadding = PaddingValues(
                        start = paddingValues.calculateStartPadding(layoutDirection),
                        end = paddingValues.calculateEndPadding(layoutDirection),
                        bottom = paddingValues.calculateBottomPadding()
                    ),
                ) {
                    item(
                        key = "Info-Box",
                        contentType = "Info-Box",
                    ) {
                        MovieInfoBox(
                            isTabletUi = false,
                            appBarPadding = topPadding,
                            title = state.movie.title,
                            author = remember(state.movie.productionCompanies) {
                                state.movie.productionCompanies?.joinToString()
                            },
                            artist = "",
                            sourceName = stringResource(id = R.string.tmdb),
                            isStubSource = false,
                            coverDataProvider = { state.movie.toPoster() },
                            status = state.movie.status,
                            onCoverClick = onPosterClick,
                            doSearch = { _, _ -> },
                        )
                    }
                    item(
                        key = "movie-actions"
                    ) {
                        ContentActionRow(
                            favorite = state.movie.favorite,
                            onAddToLibraryClicked = { contentInteractor.toggleFavorite(state.movie.toContentItem()) },
                            onAddToList = onAddToList,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item("Description-Tags") {
                        val context = LocalContext.current
                        ExpandableDescription(
                            defaultExpandState = false,
                            description = state.movie.overview,
                            tagsProvider = { state.movie.genres },
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
                        onViewClick = onViewCreditsClick,
                    )
                    trailersList(
                        trailers = state.trailers,
                        onClick = {
                            onVideoThumbnailClick(state.movie.id, true, it.id)
                        },
                        onYoutubeClick = {}
                    )
                }
            }
        }
    }
}

@Composable
fun CommentsPreview(
    count: Int,
    username: String?,
    profileImageData: UserProfileImageData?,
    lastMessage: String?,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier
            .height(72.dp)
            .fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(id = R.string.comments),
                    style = MaterialTheme.typography.labelSmall
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = remember(count) { count.toString() },
                    modifier = Modifier.alpha(0.78f),
                    style = MaterialTheme.typography.labelSmall
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            AnimatedContent(
                targetState = Triple(
                    profileImageData,
                    lastMessage,
                    username,
                ),
                label = ""
            ) { (imgData, msg, name) ->
                imgData?.let { user ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UserProfileImage(
                            data = user,
                            contentDescription = null,
                            modifier = Modifier.fillMaxHeight()
                        )
                        val secondaryContainer = MaterialTheme.colorScheme.secondaryContainer
                        Text(
                            text = name.orEmpty(),
                            maxLines = 1,
                            style = MaterialTheme.typography.labelSmall,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .padding(horizontal = 6.dp)
                                .drawWithCache {
                                    onDrawBehind {
                                        drawRoundRect(
                                            color = secondaryContainer,
                                            cornerRadius = CornerRadius(2.0.dp.toPx())
                                        )
                                    }
                                }
                                .padding(horizontal = 2.dp)
                                .weight(0.5f, false),
                        )
                        Text(
                            text = msg.orEmpty(),
                            maxLines = 1,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f, true),
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } ?: Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Text(
                        text = "Be the first to comment.",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                    )
                }
            }
        }
    }
}

@Composable
fun ContentActionRow(
    favorite: Boolean,
    onAddToLibraryClicked: () -> Unit,
    onAddToList: () -> Unit,
    modifier: Modifier = Modifier,
    onWebViewClicked: (() -> Unit)? = null,
    onWebViewLongClicked: (() -> Unit)? = null,
) {
    val defaultActionButtonColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .38f)


    Row(modifier = modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp)) {
        ContentActionButton(
            title = if (favorite) { stringResource(R.string.in_library) } else { stringResource(R.string.add_to_library) },
            icon = if (favorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            color = if (favorite) MaterialTheme.colorScheme.primary else defaultActionButtonColor,
            onClick = onAddToLibraryClicked,
            onLongClick = onAddToLibraryClicked,
        )
        ContentActionButton(
            title = stringResource(id = R.string.select_list_to_add),
            icon = Icons.Filled.AddCircleOutline,
            color = defaultActionButtonColor,
            onClick = onAddToList,
            onLongClick = onAddToList,
        )
        if (onWebViewClicked != null) {
            ContentActionButton(
                title = stringResource(R.string.action_web_view),
                icon = Icons.Outlined.Public,
                color = defaultActionButtonColor,
                onClick = onWebViewClicked,
                onLongClick = onWebViewLongClicked,
            )
        }
    }
}

@Composable
private fun RowScope.ContentActionButton(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .weight(1f)
            .combinedClickable(
                onLongClick = onLongClick,
                onClick = onClick
            ),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = title,
                color = color,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

