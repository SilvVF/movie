package io.silv.movie.presentation.screenmodel

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.silv.core_ui.components.PosterData
import io.silv.movie.data.local.CreditRepository
import io.silv.movie.data.local.LocalContentDelegate
import io.silv.movie.presentation.toPoster
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CreditsViewScreenModel(
    private val creditsRepo: CreditRepository,
    private val local: LocalContentDelegate,
    private val contentId: Long,
    private val isMovie: Boolean,
): StateScreenModel<PosterData?>(null) {

    init {
        screenModelScope.launch {
            val poster = if (isMovie) {
                local.getMovieById(contentId)?.toPoster()
            } else {
                local.getShowById(contentId)?.toPoster()
            }
            mutableState.update { poster }
        }
    }

    val credits = Pager(
        config = PagingConfig(20),
    ) {
        if (isMovie) {
            creditsRepo.movieCreditsPagingSource(contentId)
        } else {
            creditsRepo.showCreditsPagingSource(contentId)
        }
    }
        .flow.cachedIn(screenModelScope)
        .stateIn(
            screenModelScope,
            SharingStarted.WhileSubscribed(5_000),
            PagingData.empty()
        )
}