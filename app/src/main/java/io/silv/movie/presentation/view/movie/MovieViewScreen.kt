package io.silv.movie.presentation.view.movie

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import io.silv.core_ui.components.PullRefresh
import io.silv.core_ui.components.lazy.VerticalFastScroller
import io.silv.core_ui.util.copyToClipboard
import io.silv.movie.PlayerViewModel
import io.silv.movie.R
import io.silv.movie.WatchContentActivity
import io.silv.movie.data.credits.Credit
import io.silv.movie.data.lists.toContentItem
import io.silv.movie.getActivityViewModel
import io.silv.movie.presentation.LocalContentInteractor
import io.silv.movie.presentation.library.screens.AddToListScreen
import io.silv.movie.presentation.toPoster
import io.silv.movie.presentation.view.CreditsViewScreen
import io.silv.movie.presentation.view.MovieCoverScreenModel
import io.silv.movie.presentation.view.PersonViewScreen
import io.silv.movie.presentation.view.components.EditCoverAction
import io.silv.movie.presentation.view.components.ExpandableDescription
import io.silv.movie.presentation.view.components.MovieInfoBox
import io.silv.movie.presentation.view.components.PosterCoverDialog
import io.silv.movie.presentation.view.components.creditsPagingList
import io.silv.movie.presentation.view.components.trailersList
import org.koin.core.parameter.parametersOf

data class MovieViewScreen(
    val id: Long,
): Screen {

    override val key: ScreenKey
        get() = super.key + id

    @Composable
    override fun Content() {
        val screenModel = getScreenModel<MovieViewScreenModel> { parametersOf(id) }
        val mainScreenModel = getActivityViewModel<PlayerViewModel>()
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val changeDialog = remember {
            { dialog: MovieViewScreenModel.Dialog? -> screenModel.updateDialog(dialog) }
        }

        when (val state = screenModel.state.collectAsStateWithLifecycle().value) {
            MovieDetailsState.Error ->  Box(modifier = Modifier.fillMaxSize()) {
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
                    onVideoThumbnailClick = mainScreenModel::requestMediaQueue,
                    onViewCreditsClick = { navigator.push(CreditsViewScreen(state.movie.id, true)) },
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
                                .apply { putExtra("url", "https://vidsrc.to/embed/movie/${state.movie.id}") }
                        )
                    },
                    onAddToList = { navigator.push(AddToListScreen(state.movie.id, true)) }
                )
                val onDismissRequest =  { changeDialog(null) }
                when (state.dialog) {
                    null -> Unit
                    MovieViewScreenModel.Dialog.FullCover -> {
                        val sm = getScreenModel<MovieCoverScreenModel> { parametersOf(state.movie.id) }
                        val movie by sm.state.collectAsStateWithLifecycle()

                        if (movie != null) {
                            val getContent = rememberLauncherForActivityResult(
                                ActivityResultContracts.GetContent(),
                            ) {
                                if (it == null) return@rememberLauncherForActivityResult
                                sm.editCover(context, it)
                            }
                            val poster = remember(movie) { movie!!.toPoster() }
                            PosterCoverDialog(
                                coverDataProvider = { poster },
                                isCustomCover = remember(movie) { sm.hasCustomCover(movie!!) },
                                onShareClick = { sm.shareCover(context) },
                                onSaveClick = { sm.saveCover(context) },
                                snackbarHostState = sm.snackbarHostState,
                                onEditClick = if (movie!!.favorite || movie!!.inList) {
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
                }
            }
        }
    }
}

@Composable
fun MovieDetailsContent(
    state: MovieDetailsState.Success,
    creditsProvider: () -> LazyPagingItems<Credit>,
    refresh: () -> Unit,
    onPosterClick: () -> Unit,
    onViewCreditsClick: () -> Unit,
    onVideoThumbnailClick: (movieId: Long, isMovie: Boolean,  trailerId: String) -> Unit,
    onCreditClick: (credit: Credit) -> Unit,
    onWatchMovieClick: () -> Unit,
    onAddToList: () -> Unit,
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
                        key = "movie-actions"
                    ) {
                        ContentActionRow(
                            favorite = state.movie.favorite,
                            onAddToLibraryClicked = { contentInteractor.toggleFavorite(state.movie.toContentItem()) },
                            onAddToList = onAddToList,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
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

