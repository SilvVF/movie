package io.silv.movie.presentation.covers.screenmodel

import android.content.Context
import android.net.Uri
import androidx.compose.material3.SnackbarHostState
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import coil.imageLoader
import coil.request.ImageRequest
import coil.size.Size
import io.silv.core_ui.voyager.ioCoroutineScope
import io.silv.movie.R
import io.silv.movie.core.ImageUtil
import io.silv.movie.data.model.Movie
import io.silv.movie.data.local.MovieRepository
import io.silv.movie.data.local.awaitUpdateCoverLastModified
import io.silv.movie.presentation.covers.Image
import io.silv.movie.presentation.covers.ImageSaver
import io.silv.movie.presentation.covers.Location
import io.silv.movie.presentation.covers.cache.MovieCoverCache
import io.silv.movie.presentation.toPoster
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class MovieCoverScreenModel(
    private val movieId: Long,
    private val movieRepo: MovieRepository,
    private val imageSaver: ImageSaver,
    private val coverCache: MovieCoverCache,
) : StateScreenModel<Movie?>(null) {

    val snackbarHostState: SnackbarHostState = SnackbarHostState()

    init {
        ioCoroutineScope.launch {
            movieRepo.observeMovieById(movieId)
                .collect { newManga -> mutableState.update { newManga } }
        }
    }

    fun saveCover(context: Context) {
        screenModelScope.launch {
            try {
                saveCoverInternal(context, temp = false)
                snackbarHostState.showSnackbar(
                    context.getString(R.string.error),
                    withDismissAction = true,
                )
            } catch (e: Throwable) {
                Timber.e(e)
                snackbarHostState.showSnackbar(
                    context.getString(R.string.error),
                    withDismissAction = true,
                )
            }
        }
    }

    fun shareCover(context: Context) {
        screenModelScope.launch {
            try {
                val uri = saveCoverInternal(context, temp = true) ?: return@launch
                withContext(Dispatchers.Main) {
                    context.startActivity(ImageUtil.toShareIntent(context, uri))
                }
            } catch (e: Throwable) {
                Timber.e(e)
                snackbarHostState.showSnackbar(
                    context.getString(R.string.error),
                    withDismissAction = true,
                )
            }
        }
    }

    fun hasCustomCover(movie: Movie): Boolean {
        return coverCache.getCustomCoverFile(movie.id).exists()
    }

    /**
     * Save manga cover Bitmap to picture or temporary share directory.
     *
     * @param context The context for building and executing the ImageRequest
     * @return the uri to saved file
     */
    private suspend fun saveCoverInternal(context: Context, temp: Boolean): Uri? {
        val movie = state.value ?: return null
        val req = ImageRequest.Builder(context)
            .data(movie.toPoster())
            .size(Size.ORIGINAL)
            .build()

        return withContext(Dispatchers.IO) {
            val result = context.imageLoader.execute(req).drawable

            val bitmap = ImageUtil.getBitmapOrNull(result) ?: return@withContext null
            imageSaver.save(
                Image.Cover(
                    bitmap = bitmap,
                    name = movie.title,
                    location = if (temp) Location.Cache else Location.Pictures.create(),
                ),
            )
        }
    }

    /**
     * Update cover with local file.
     *
     * @param context Context.
     * @param data uri of the cover resource.
     */
    fun editCover(context: Context, data: Uri) {
        val movie = state.value ?: return
        ioCoroutineScope.launch {
            context.contentResolver.openInputStream(data)?.use { ins ->
                try {
                    coverCache.setCustomCoverToCache(movie, ins)
                    movieRepo.awaitUpdateCoverLastModified(movie.id)
                    notifyCoverUpdated(context)
                } catch (e: Exception) {
                    notifyFailedCoverUpdate(context, e)
                }
            }
        }
    }

    fun deleteCustomCover(context: Context) {
        val mangaId = state.value?.id ?: return
        ioCoroutineScope.launch {
            try {
                coverCache.deleteCustomCover(mangaId)
                movieRepo.awaitUpdateCoverLastModified(mangaId)
                notifyCoverUpdated(context)
            } catch (e: Exception) {
                notifyFailedCoverUpdate(context, e)
            }
        }
    }

    private fun notifyCoverUpdated(context: Context) {
        screenModelScope.launch {
            snackbarHostState.showSnackbar(
                context.getString(R.string.cover_updated),
                withDismissAction = true,
            )
        }
    }

    private fun notifyFailedCoverUpdate(context: Context, e: Throwable) {
        screenModelScope.launch {
            snackbarHostState.showSnackbar(
                context.getString(R.string.notification_cover_update_failed),
                withDismissAction = true,
            )
            Timber.e(e)
        }
    }
}