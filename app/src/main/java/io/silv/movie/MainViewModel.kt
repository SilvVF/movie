package io.silv.movie

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cafe.adriel.voyager.navigator.Navigator
import io.github.jan.supabase.auth.Auth
import io.silv.movie.api.service.piped.PipedApi
import io.silv.movie.data.DeleteContentList
import io.silv.movie.data.EditContentList
import io.silv.movie.data.ListUpdateManager
import io.silv.movie.data.local.ContentListRepository
import io.silv.movie.data.local.LocalContentDelegate
import io.silv.movie.data.local.TrailerRepository
import io.silv.movie.data.network.NetworkContentDelegate
import io.silv.movie.data.supabase.BackendRepository
import io.silv.movie.prefrences.UiPreferences
import io.silv.movie.presentation.ContentInteractor
import io.silv.movie.presentation.ListInteractor
import io.silv.movie.presentation.covers.cache.MovieCoverCache
import io.silv.movie.presentation.covers.cache.TVShowCoverCache
import io.silv.movie.presentation.media.PlayerPresenter
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.flow.stateIn

@Immutable
@Stable
class MainViewModel(
    movieCoverCache: MovieCoverCache,
    tvShowCoverCache: TVShowCoverCache,
    editContentList: EditContentList,
    deleteContentList: DeleteContentList,
    contentListRepository: ContentListRepository,
    local: LocalContentDelegate,
    trailerRepository: TrailerRepository,
    network: NetworkContentDelegate,
    backendRepository: BackendRepository,
    listUpdateManager: ListUpdateManager,
    pipedApi: PipedApi,
    uiPreferences: UiPreferences,
    auth: Auth,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val navigationChannel: Channel<Navigator.() -> Unit> = Channel(10)
    val snackbarHostState = SnackbarHostState()

    private val appStateProvider = AppStateProvider(uiPreferences)

    val state = appStateProvider.observeAppData
        .retry()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            AppState.Loading
        )

    val playerPresenter = PlayerPresenter(
        trailerRepository,
        pipedApi,
        viewModelScope,
        savedStateHandle
    )

    val contentInteractor = ContentInteractor.default(
        local,
        backendRepository,
        contentListRepository,
        network,
        auth,
        movieCoverCache,
        tvShowCoverCache,
        viewModelScope
    )

    val listInteractor = ListInteractor.default(
        contentListRepository,
        backendRepository,
        listUpdateManager,
        editContentList,
        deleteContentList,
        movieCoverCache,
        tvShowCoverCache,
        auth,
        viewModelScope
    )
}