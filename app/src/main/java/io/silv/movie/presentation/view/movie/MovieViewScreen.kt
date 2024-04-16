package io.silv.movie.presentation.view.movie

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
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
import io.silv.movie.data.credits.Credit
import io.silv.movie.getActivityViewModel
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
                    }
                )
                val onDismissRequest =  { changeDialog(null) }
                when (state.dialog) {
                    null -> Unit
                    MovieViewScreenModel.Dialog.FullCover -> {
                        val sm = getScreenModel<MovieCoverScreenModel> { parametersOf(state.movie.id) }
                        val movie by sm.state.collectAsStateWithLifecycle()
                        val context = LocalContext.current

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
) {
    Scaffold { paddingValues ->

        val topPadding = paddingValues.calculateTopPadding()
        val listState = rememberLazyListState()
        val hazeState = remember { HazeState() }

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

