package io.silv.movie.presentation.media

import android.net.Uri
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import io.silv.movie.data.trailers.Trailer
import io.silv.movie.network.model.Streams
import io.silv.movie.network.model.Subtitle
import io.silv.movie.network.service.piped.PipedApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import org.burnoutcrew.reorderable.ItemPosition
import kotlin.time.Duration.Companion.seconds

class PipedApiViewModel(
    private val pipedApi: PipedApi
): ViewModel() {

    var player: ExoPlayer? = null

    var lastTrailer: Trailer? = null
    private var trailerToStreams by mutableStateOf<Pair<Trailer,Streams>?>(null)


    val trailerQueue = mutableStateListOf<Trailer>()

    val streams by derivedStateOf { trailerToStreams?.second }
    val currentTrailer by  derivedStateOf { trailerQueue.firstOrNull() }

    init {
        viewModelScope.launch {
            snapshotFlow { currentTrailer }
                .filterNotNull()
                .distinctUntilChanged()
                .debounce {
                    if (streams == null) 0.seconds else 3.seconds
                }
                .collectLatest { trailer ->
                    if (trailer == trailerToStreams?.first)
                        return@collectLatest

                    trailerToStreams = null
                    trailerToStreams = trailer to pipedApi.getStreams(trailer.key)
                }
        }
    }

    fun initialize(trailers: List<Trailer>) {

        if (trailerQueue.isNotEmpty()) {
            trailers.forEach { trailer ->
               if (!trailerQueue.contains(trailer)) {
                   trailerQueue.add(trailer)
               }
            }
            return
        }

        trailerQueue.addAll(trailers)
    }

    fun onDragEnd(from: Int, to: Int) {

    }

    fun onMove(from: ItemPosition, to: ItemPosition) {
        if (from.index >= trailerQueue.size) { return }

        trailerQueue.add(to.index, trailerQueue.removeAt(from.index))
    }

    private fun getSubtitleConfigs(): List<MediaItem.SubtitleConfiguration> = streams!!.subtitles.map {
        val roleFlags = getSubtitleRoleFlags(it)
        MediaItem.SubtitleConfiguration.Builder(it.url!!.toUri())
            .setRoleFlags(roleFlags)
            .setLanguage(it.code)
            .setMimeType(it.mimeType).build()
    }

    fun createMediaItem(uri: Uri, mimeType: String) = MediaItem.Builder()
        .setUri(uri)
        .setMimeType(mimeType)
        .setSubtitleConfigurations(getSubtitleConfigs())
        .setMetadata(streams!!)
        .build()

    private fun getSubtitleRoleFlags(subtitle: Subtitle?): Int {
        return if (subtitle?.autoGenerated != true) {
            C.ROLE_FLAG_CAPTION
        } else {
            PlayerHelper.ROLE_FLAG_AUTO_GEN_SUBTITLE
        }
    }

    private fun MediaItem.Builder.setMetadata(streams: Streams) = apply {
        setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(streams.title)
                .setArtist(streams.uploader)
                .setArtworkUri(streams.thumbnailUrl.toUri())
                .build()
        )
    }
}
