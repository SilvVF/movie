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

    private var contentId: Long? = savedStateHandle["content"]
        set(value) {
            savedStateHandle["content"] = value
            field = value
        }
    private var isMovie: Boolean? = savedStateHandle["content"]
        set(value) {
            savedStateHandle["content"] = value
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
                contentId ?: return@run,
                isMovie ?: return@run,
                trailerId ?: return@run
            )
        }
    }

    fun requestMediaQueue(contentId: Long, isMovie: Boolean, tid: Long) {

        this.contentId = contentId
        this.isMovie = isMovie
        trailerId = tid

        viewModelScope.launch {

            val trailers = if (isMovie) {
                trailerRepository.getTrailersByMovieId(contentId)
            } else {
                trailerRepository.getTrailersByShowId(contentId)
            }

            val mutableTrailers =  trailers.toMutableList()

            val trailerIdx = trailers.indexOfFirst { it.id == tid }

            if (trailerIdx != -1) {
                mutableTrailers.add(0, mutableTrailers.removeAt(trailerIdx))
            }
            videos = mutableTrailers.toImmutableList()
        }
    }

    fun clearMediaQueue() {
        videos = null
        viewModelScope.launch {
            collapsableVideoState?.state?.snapTo(CollapsableVideoAnchors.Start)
        }
    }
}
