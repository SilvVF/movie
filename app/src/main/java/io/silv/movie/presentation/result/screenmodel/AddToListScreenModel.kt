package io.silv.movie.presentation.result.screenmodel

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.github.jan.supabase.gotrue.Auth
import io.silv.core_ui.components.PosterData
import io.silv.movie.data.content.lists.repository.ContentListRepository
import io.silv.movie.data.content.movie.interactor.GetMovie
import io.silv.movie.data.content.tv.interactor.GetShow
import io.silv.movie.presentation.toPoster
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class AddToListScreenModel(
    contentListRepository: ContentListRepository,
    getMovie: GetMovie,
    getShow: GetShow,
    auth: Auth,
    contentId: Long,
    isMovie: Boolean,
): StateScreenModel<PosterData?>(null) {

    init {
        val posterDataFlow = if (isMovie) {
            getMovie.subscribePartial(contentId)
                .onEach { movie ->
                    mutableState.update { movie.toPoster() }
                }
        } else {
            getShow.subscribePartial(contentId)
                .onEach { show ->
                    mutableState.update { show.toPoster() }
                }
        }

        posterDataFlow.launchIn(screenModelScope)
    }

    val lists = contentListRepository.observeLibraryItems("")
        .map { contentListItems ->
            contentListItems
                .filter { it.first.createdBy == null || it.first.createdBy == auth.currentUserOrNull()?.id }
                .filterNot { (_, items) ->
                    items.any { item ->
                        item.isMovie == isMovie && item.contentId == contentId
                    }
                }
                .map { it.first to it.second.toImmutableList() }
                .toImmutableList()
        }
        .stateIn(
            screenModelScope,
            SharingStarted.WhileSubscribed(5_000),
            persistentListOf()
        )
}