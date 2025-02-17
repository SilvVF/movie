package io.silv.movie.data.workers

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Operation
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.github.jan.supabase.auth.Auth
import io.silv.movie.R
import io.silv.movie.data.local.ContentListRepository
import io.silv.movie.data.local.LocalContentDelegate
import io.silv.movie.data.model.Movie
import io.silv.movie.data.model.TVShow
import io.silv.movie.data.model.toUpdate
import io.silv.movie.data.supabase.ListRepository
import io.silv.movie.data.supabase.BackendRepository
import kotlinx.datetime.Clock
import timber.log.Timber

/**
 * Toggles favorite for movies and shows from the list in supabase.
 * Fetches the movie or show if it does not exist already.
 * No deletion / removals of favorites happen only additions for current signed in user.
 */

class ListUpdateWorker (
    private val listRepository: ListRepository,
    private val backendRepository: BackendRepository,
    private val contentListRepository: ContentListRepository,
    private val localContent: LocalContentDelegate,
    private val auth: Auth,
    appContext: Context,
    params: WorkerParameters
): CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {

        val listId = inputData.getString(KEY_LIST_ID)!!

        val list = listRepository.selectListWithItemsById(listId).getOrThrow()
        val username = backendRepository.getUser(list.userId).getOrThrow().username
        val isOwnerMe = list.userId == auth.currentUserOrNull()?.id

        return try {
            val local = contentListRepository.getListForSupabaseId(list.listId) ?: run {
                val id = contentListRepository.createList(
                    name = list.name,
                    supabaseId = list.listId,
                    userId = list.userId,
                    createdAt = list.createdAt.epochSeconds,
                    inLibrary = isOwnerMe,
                    subscribers = list.subscribers
                )
               contentListRepository.getList(id)!!
            }

            contentListRepository.updateList(
                local.copy(
                    description = list.description,
                    lastModified = list.updatedAt?.epochSeconds ?: local.lastModified,
                    name = list.name,
                    public = list.public,
                    createdBy = list.userId,
                    lastSynced = Clock.System.now().epochSeconds,
                    username = username,
                    inLibrary = isOwnerMe,
                    subscribers = list.subscribers
                )
                    .toUpdate()
            )

            val items = list.items.orEmpty()

            for (item in items) {
                if (item.movieId != -1L) {
                    var movie = localContent.getMovieById(item.movieId)
                    if (movie == null) {
                        val id = localContent.insertMovie(
                            Movie.create().copy(
                                id = item.movieId,
                                title = item.title,
                                overview = item.description.orEmpty(),
                                posterUrl = item.posterPath.takeIf { !item.posterPath.isNullOrBlank() },
                            )
                        )
                        movie = localContent.getMovieById(id!!)!!
                    }
                    contentListRepository.addMovieToList(movie.id, local, item.createdAt.epochSeconds)
                } else if (item.showId != -1L) {
                    var show = localContent.getShowById(item.showId)
                    if (show == null) {
                        val id = localContent.insertShow(
                            TVShow.create().copy(
                                id = item.showId,
                                title = item.title,
                                overview = item.description.orEmpty(),
                                posterUrl =  item.posterPath.takeIf { !item.posterPath.isNullOrBlank() },
                            )
                        )
                        show = localContent.getShowById(id!!)!!
                    }
                    contentListRepository.addShowToList(show.id, local, item.createdAt.epochSeconds)
                }
            }
            Result.success()
        } catch (ignored: Exception){
            Timber.e(ignored)
            Result.failure()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return createForegroundInfo(inputData.getString(KEY_LIST_ID).orEmpty())
    }

    private fun createForegroundInfo(listId: String): ForegroundInfo {
        val id = BASE_TAG + listId
        val title = "Refreshing list $listId"
        val cancel = "cancel"
        // This PendingIntent can be used to cancel the worker
        val intent = WorkManager.getInstance(applicationContext)
            .createCancelPendingIntent(getId())

        val notification = NotificationCompat.Builder(applicationContext, id)
            .setContentTitle(title)
            .setContentText("refreshing recommendations")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            // Add the cancel action to the notification which can
            // be used to cancel the worker
            .addAction(android.R.drawable.ic_delete, cancel, intent)
            .build()

        return ForegroundInfo(1133, notification)
    }


    companion object {
        const val BASE_TAG = "io.silv.ListUpdateWorker"
        const val KEY_LIST_ID = "list_id"

        fun start(context: Context, listId: String): Operation {
            val workManager = WorkManager.getInstance(context)

            return workManager.enqueueUniqueWork(
                BASE_TAG + listId,
                ExistingWorkPolicy.REPLACE,
                workRequest(listId)
            )
        }

        fun workRequest(listId: String) = OneTimeWorkRequestBuilder<ListUpdateWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setInputData(
                Data.Builder()
                    .putString(KEY_LIST_ID, listId)
                    .build()
            )
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .addTag(BASE_TAG + listId)
            .build()
    }
}