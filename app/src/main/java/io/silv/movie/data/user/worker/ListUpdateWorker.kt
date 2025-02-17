package io.silv.movie.data.user.worker

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.github.jan.supabase.gotrue.Auth
import io.silv.movie.R
import io.silv.movie.data.content.lists.ContentListRepository
import io.silv.movie.data.content.lists.toUpdate
import io.silv.movie.data.content.movie.model.Movie
import io.silv.movie.data.content.movie.local.MovieRepository
import io.silv.movie.data.content.movie.model.TVShow
import io.silv.movie.data.content.movie.local.ShowRepository
import io.silv.movie.data.user.repository.ListRepository
import io.silv.movie.data.user.repository.UserRepository
import kotlinx.datetime.Clock
import timber.log.Timber

/**
 * Toggles favorite for movies and shows from the list in supabase.
 * Fetches the movie or show if it does not exist already.
 * No deletion / removals of favorites happen only additions for current signed in user.
 */
class ListUpdateWorker (
    private val listUpdater: ListUpdater,

    appContext: Context,
    private val params: WorkerParameters
): CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {

        listUpdater.await(inputData.getString(KEY_LIST_ID)!!)

        return Result.success()
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

        fun start(context: Context, listId: String) {
            val workManager = WorkManager.getInstance(context)

            workManager.enqueueUniqueWork(
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

class ListUpdater(
    private val contentListRepository: ContentListRepository,
    private val listRepository: ListRepository,
    private val movieRepository: MovieRepository,
    private val showRepository: ShowRepository,
    private val userRepository: UserRepository,
    private val auth: Auth,
) {

    suspend fun await(listId: String) {

        val list = listRepository.selectListWithItemsById(listId)!!
        val username = userRepository.getUser(list.userId)?.username
        val isOwnerMe = list.userId == auth.currentUserOrNull()?.id

        try {
            var local = contentListRepository.getListForSupabaseId(list.listId)

            if (local == null) {
                val id = contentListRepository.createList(
                    name = list.name,
                    supabaseId = list.listId,
                    userId = list.userId,
                    createdAt = list.createdAt.epochSeconds,
                    inLibrary = isOwnerMe,
                    subscribers = list.subscribers
                )
                local = contentListRepository.getList(id)!!
            }

            contentListRepository.updateList(
                local.copy(
                    description = list.description,
                    lastModified = list.updatedAt?.epochSeconds ?: local.lastModified,
                    name = list.name,
                    public = list.public,
                    createdBy = list.userId,
                    lastSynced = Clock.System.now().epochSeconds,
                    username = username ?: local.username,
                    inLibrary = isOwnerMe,
                    subscribers = list.subscribers
                )
                    .toUpdate()
            )

            val items = list.items.orEmpty()

            for (item in items) {
                if (item.movieId != -1L) {
                    var movie = movieRepository.getMovieById(item.movieId)
                    if (movie == null) {
                        val id = movieRepository.insertMovie(
                            Movie.create().copy(
                                id = item.movieId,
                                title = item.title,
                                overview = item.description.orEmpty(),
                                posterUrl = item.posterPath.takeIf { !item.posterPath.isNullOrBlank() },
                            )
                        )
                        movie = movieRepository.getMovieById(id!!)!!
                    }
                    contentListRepository.addMovieToList(movie.id, local, item.createdAt.epochSeconds)
                } else if (item.showId != -1L) {
                    var show = showRepository.getShowById(item.showId)
                    if (show == null) {
                        val id = showRepository.insertShow(
                            TVShow.create().copy(
                                id = item.showId,
                                title = item.title,
                                overview = item.description.orEmpty(),
                                posterUrl =  item.posterPath.takeIf { !item.posterPath.isNullOrBlank() },
                            )
                        )
                        show = showRepository.getShowById(id!!)!!
                    }
                    contentListRepository.addShowToList(show.id, local, item.createdAt.epochSeconds)
                }
            }

        } catch (ignored: Exception){ Timber.e(ignored) }
    }
}