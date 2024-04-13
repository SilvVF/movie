package io.silv.movie

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.gotrue.Auth
import io.silv.movie.data.cache.MovieCoverCache
import io.silv.movie.data.cache.TVShowCoverCache
import io.silv.movie.data.lists.ContentListRepository
import io.silv.movie.data.lists.interactor.AddContentItemToList
import io.silv.movie.data.lists.interactor.DeleteContentList
import io.silv.movie.data.lists.interactor.EditContentList
import io.silv.movie.data.lists.interactor.RemoveContentItemFromList
import io.silv.movie.data.lists.interactor.ToggleContentItemFavorite
import io.silv.movie.data.user.ListRepository
import io.silv.movie.data.user.ListUpdater
import io.silv.movie.presentation.DefaultContentInteractor
import io.silv.movie.presentation.DefaultListInteractor

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

    val contentInteractor = DefaultContentInteractor(
        toggleContentItemFavorite,
        removeContentItemFromList,
        addContentItemToList,
        contentListRepository,
        auth,
        movieCoverCache,
        tvShowCoverCache,
        viewModelScope
    )

    val listInteractor = DefaultListInteractor(
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