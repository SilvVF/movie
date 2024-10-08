package io.silv.movie

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cafe.adriel.voyager.navigator.Navigator
import io.github.jan.supabase.gotrue.Auth
import io.silv.movie.data.content.lists.interactor.AddContentItemToList
import io.silv.movie.data.content.lists.interactor.DeleteContentList
import io.silv.movie.data.content.lists.interactor.EditContentList
import io.silv.movie.data.content.lists.interactor.RemoveContentItemFromList
import io.silv.movie.data.content.lists.interactor.ToggleContentItemFavorite
import io.silv.movie.data.content.lists.repository.ContentListRepository
import io.silv.movie.data.user.repository.ListRepository
import io.silv.movie.data.user.worker.ListUpdater
import io.silv.movie.presentation.ContentInteractor
import io.silv.movie.presentation.ListInteractor
import io.silv.movie.presentation.covers.cache.MovieCoverCache
import io.silv.movie.presentation.covers.cache.TVShowCoverCache
import kotlinx.coroutines.channels.Channel

@Immutable
@Stable
class MainScreenModel(
    toggleContentItemFavorite: ToggleContentItemFavorite,
    removeContentItemFromList: RemoveContentItemFromList,
    addContentItemToList: AddContentItemToList,
    movieCoverCache: MovieCoverCache,
    tvShowCoverCache: TVShowCoverCache,
    editContentList: EditContentList,
    contentListRepository: ContentListRepository,
    listRepository: ListRepository,
    deleteContentList: DeleteContentList,
    listUpdater: ListUpdater,
    auth: Auth
): ViewModel() {

    val navigationChannel: Channel<Navigator.() -> Unit> = Channel(10)

    val contentInteractor = ContentInteractor.default(
        toggleContentItemFavorite,
        removeContentItemFromList,
        addContentItemToList,
        contentListRepository,
        auth,
        movieCoverCache,
        tvShowCoverCache,
        viewModelScope
    )

    val listInteractor = ListInteractor.default(
        contentListRepository,
        listRepository,
        listUpdater,
        addContentItemToList,
        editContentList,
        deleteContentList,
        movieCoverCache,
        tvShowCoverCache,
        auth,
        viewModelScope
    )
}