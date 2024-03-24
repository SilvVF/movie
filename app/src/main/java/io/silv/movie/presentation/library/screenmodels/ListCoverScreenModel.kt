package io.silv.movie.presentation.library.screenmodels

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
import io.silv.movie.data.cache.ListCoverCache
import io.silv.movie.data.lists.ContentList
import io.silv.movie.data.lists.ContentListRepository
import io.silv.movie.data.lists.ContentListUpdate
import io.silv.movie.data.lists.toUpdate
import io.silv.movie.presentation.view.Image
import io.silv.movie.presentation.view.ImageSaver
import io.silv.movie.presentation.view.Location
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import timber.log.Timber
import java.io.InputStream

class ListCoverScreenModel(
    private val listId: Long,
    private val contentListRepository: ContentListRepository,
    private val coverCache: ListCoverCache,
    private val imageSaver: ImageSaver
) : StateScreenModel<ContentList?>(null) {

    val snackbarHostState: SnackbarHostState = SnackbarHostState()

    var job: Job? = null

    init {
        job = ioCoroutineScope.launch {
            contentListRepository.observeListById(listId)
                .collect { newManga ->
                    mutableState.update { newManga } }
        }
    }

    fun refresh(id: Long) {
        job?.cancel()
        job = ioCoroutineScope.launch {
            contentListRepository.observeListById(id)
                .collect { newManga ->
                    mutableState.update { newManga } }
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

    fun hasCustomCover(movie: ContentList): Boolean {
        return coverCache.getCustomCoverFile(movie.id).exists()
    }

    /**
     * Save manga cover Bitmap to picture or temporary share directory.
     *
     * @param context The context for building and executing the ImageRequest
     * @return the uri to saved file
     */
    private suspend fun saveCoverInternal(context: Context, temp: Boolean): Uri? {
        val list = state.value ?: return null
        val req = ImageRequest.Builder(context)
            .data(coverCache.getCustomCoverFile(list.id))
            .size(Size.ORIGINAL)
            .build()

        return withContext(Dispatchers.IO) {
            val result = context.imageLoader.execute(req).drawable

            val bitmap = ImageUtil.getBitmapOrNull(result) ?: return@withContext null
            imageSaver.save(
                Image.Cover(
                    bitmap = bitmap,
                    name = list.name,
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
            context.contentResolver.openInputStream(data)?.use {
                try {
                    movie.editCover(it, coverCache)
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
                contentListRepository.updateList(
                    ContentListUpdate(
                        id = mangaId,
                        posterLastUpdated = Clock.System.now().toEpochMilliseconds()
                    )
                )
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

    private suspend fun ContentList.editCover(
        stream: InputStream,
        coverCache: ListCoverCache,
    ) {
        coverCache.setCustomCoverToCache(this, stream)
        contentListRepository.updateList(
            copy(posterLastModified = Clock.System.now().toEpochMilliseconds()).toUpdate()
        )
    }

    override fun onDispose() {
        job?.cancel()
        job = null
    }
}