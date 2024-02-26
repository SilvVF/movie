package io.silv.movie.presentation.media

import android.content.Context
import android.view.View
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

class CustomPlayer(
    private val context: Context,
    private val player: YouTubePlayer,
    private val playerView: YouTubePlayerView,
    private val panel: View
): AbstractYouTubePlayerListener() {

}