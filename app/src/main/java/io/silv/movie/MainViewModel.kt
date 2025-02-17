package io.silv.movie

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cafe.adriel.voyager.navigator.Navigator
import io.github.jan.supabase.auth.Auth
import io.silv.movie.data.DeleteContentList
import io.silv.movie.data.EditContentList
import io.silv.movie.data.local.ContentListRepository
import io.silv.movie.data.local.LocalContentDelegate
import io.silv.movie.data.network.NetworkContentDelegate
import io.silv.movie.data.ListUpdateManager
import io.silv.movie.data.supabase.BackendRepository
import io.silv.movie.presentation.ContentInteractor
import io.silv.movie.presentation.ListInteractor
import io.silv.movie.presentation.covers.cache.MovieCoverCache
import io.silv.movie.presentation.covers.cache.TVShowCoverCache
import kotlinx.coroutines.channels.Channel

@Immutable
@Stable
class MainViewModel(
    movieCoverCache: MovieCoverCache,
    tvShowCoverCache: TVShowCoverCache,
    editContentList: EditContentList,
    contentListRepository: ContentListRepository,
    local: LocalContentDelegate,
    network: NetworkContentDelegate,
    backendRepository: BackendRepository,
    deleteContentList: DeleteContentList,
    listUpdateManager: ListUpdateManager,
    auth: Auth
): ViewModel() {

    val navigationChannel: Channel<Navigator.() -> Unit> = Channel(10)

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