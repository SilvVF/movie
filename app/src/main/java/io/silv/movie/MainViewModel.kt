package io.silv.movie

import androidx.compose.foundation.gestures.snapTo
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.silv.movie.data.trailers.Trailer
import io.silv.movie.data.trailers.TrailerRepository
import io.silv.movie.presentation.media.CollapsableVideoAnchors
import io.silv.movie.presentation.media.CollapsableVideoState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch


class MainViewModel(
    private val trailerRepository: TrailerRepository,
    private val savedStateHandle: SavedStateHandle
): ViewModel() {


    var videos by mutableStateOf<ImmutableList<Trailer>?>(null)
        private set


    var collapsableVideoState: CollapsableVideoState? = null

    private var movieId: Long? = savedStateHandle["movie"]
        set(value) {
            savedStateHandle["movie"] = value
            field = value
        }
    private var trailerId: Long?  = savedStateHandle["trailer"]
        set(value) {
            savedStateHandle["trailer"] = value
            field = value
        }

    init {
        run {
            requestMediaQueue(
                movieId ?: return@run,
                trailerId ?: return@run,
            )
        }
    }

    fun requestMediaQueue(mid: Long, tid: Long) {

        movieId = mid
        trailerId = tid

        viewModelScope.launch {
            val trailers = trailerRepository.getTrailersByMovieId(mid)
            videos = trailers.toImmutableList()
        }
    }

    fun clearMediaQueue() {
        videos = null
        viewModelScope.launch {
            collapsableVideoState?.state?.snapTo(CollapsableVideoAnchors.Start)
        }
    }
}
