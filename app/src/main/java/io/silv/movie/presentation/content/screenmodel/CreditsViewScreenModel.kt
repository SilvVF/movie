package io.silv.movie.presentation.content.screenmodel

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.silv.core_ui.components.PosterData
import io.silv.movie.data.content.credits.CreditRepository
import io.silv.movie.data.content.movie.interactor.GetMovie
import io.silv.movie.data.content.tv.interactor.GetShow
import io.silv.movie.presentation.toPoster
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CreditsViewScreenModel(
    private val creditsRepository: CreditRepository,
    private val getMovie: GetMovie,
    private val getShow: GetShow,
    private val contentId: Long,
    private val isMovie: Boolean,
): StateScreenModel<PosterData?>(null) {

    init {
        screenModelScope.launch {
            val poster = if (isMovie) {
                getMovie.await(contentId)?.toPoster()
            } else {
                getShow.await(contentId)?.toPoster()
            }
            mutableState.update { poster }
        }
    }

    val credits = Pager(
        config = PagingConfig(20),
    ) {
        if (isMovie) {
            creditsRepository.movieCreditsPagingSource(contentId)
        } else {
            creditsRepository.showCreditsPagingSource(contentId)
        }
    }
        .flow.cachedIn(screenModelScope)
        .stateIn(
            screenModelScope,
            SharingStarted.WhileSubscribed(5_000),
            PagingData.empty()
        )
}