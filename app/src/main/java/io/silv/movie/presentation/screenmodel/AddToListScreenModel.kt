package io.silv.movie.presentation.screenmodel

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.github.jan.supabase.auth.Auth
import io.silv.core_ui.components.PosterData
import io.silv.movie.data.local.ContentListRepository
import io.silv.movie.data.local.LocalContentDelegate
import io.silv.movie.presentation.toPoster
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class AddToListScreenModel(
    contentListRepository: ContentListRepository,
    local: LocalContentDelegate,
    auth: Auth,
    contentId: Long,
    isMovie: Boolean,
): StateScreenModel<PosterData?>(null) {

    init {
        if (isMovie) {
            local.observeMoviePartialById(contentId)
                .onEach { movie ->
                    mutableState.emit(movie?.toPoster())
                }
        } else {
            local.observeShowPartialById(contentId)
                .onEach { show ->
                    mutableState.emit(show?.toPoster())
                }
        }
            .launchIn(screenModelScope)
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
        }
        .stateIn(
            screenModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )
}