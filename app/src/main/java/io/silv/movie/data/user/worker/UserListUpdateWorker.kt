package io.silv.movie.data.user.worker

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
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
import io.silv.movie.data.content.movie.local.LocalContentDelegate
import io.silv.movie.data.content.movie.local.networkToLocalMovie
import io.silv.movie.data.content.movie.local.networkToLocalShow
import io.silv.movie.data.content.movie.model.toDomain
import io.silv.movie.data.content.movie.network.NetworkContentDelegate
import io.silv.movie.data.user.repository.ListRepository
import io.silv.movie.data.user.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import kotlinx.datetime.Clock
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

/**
 * Toggles favorite for movies and shows from the list in supabase.
 * Fetches the movie or show if it does not exist already.
 * No deletion / removals of favorites happen only additions for current signed in user.
 */
class UserListUpdateWorker (
    appContext: Context,
    private val params: WorkerParameters
): CoroutineWorker(appContext, params), KoinComponent {

    private val contentListRepository: ContentListRepository by inject()
    private val listRepository: ListRepository by inject()
    private val auth: Auth by inject()
    private val local: LocalContentDelegate by inject()
    private val network: NetworkContentDelegate by inject()
    private val userRepository: UserRepository by inject()


    override suspend fun doWork(): Result {

        val user = auth.currentUserOrNull()!!

        supervisorScope {

            val subscriptions = async(Dispatchers.IO) {
                listRepository.selectSubscriptions(user.id)
            }

            val userCreated  = async(Dispatchers.IO) {
                listRepository.selectListsByUserId(user.id)
            }

            val username = async(Dispatchers.IO) {
                userRepository.getUser(user.id)?.username
            }

            val networkLists = userCreated.await().orEmpty() + subscriptions.await().orEmpty()

            for (list in networkLists) {
                try {
                    var localList = contentListRepository.getListForSupabaseId(list.listId)

                    if (localList == null) {
                        val id = contentListRepository.createList(
                            name = list.name,
                            supabaseId = list.listId,
                            userId = list.userId,
                            inLibrary = true,
                            subscribers = list.subscribers,
                            createdAt = list.createdAt.epochSeconds
                        )
                        localList = contentListRepository.getList(id)
                            ?: error("failed to add list $networkLists")
                    }

                    Timber.d(localList.toString())

                    contentListRepository.updateList(
                        localList.copy(
                            description = list.description,
                            name = list.name,
                            username = username.await() ?: localList.username,
                            public = list.public,
                            inLibrary = true,
                            subscribers = list.subscribers,
                            lastSynced = Clock.System.now().toEpochMilliseconds()
                        )
                            .toUpdate()
                    )


                    val items = list.items.orEmpty()

                    val addToList = mutableListOf<Pair<Long, Boolean>>()

                    for (item in items) {
                        try {
                            if (item.movieId != -1L) {
                                var movie = local.getMovieById(item.movieId)
                                if (movie == null) {
                                    movie = local.networkToLocalMovie(
                                        network.getMovie(item.movieId)!!.toDomain()
                                    )
                                }
                                addToList.add(movie.id to true)
                            } else if (item.showId != -1L) {
                                var show = local.getShowById(item.showId)
                                if (show == null) {
                                    show = local.networkToLocalShow(
                                        network.getShow(item.showId)!!.toDomain()
                                    )
                                }
                                addToList.add(show.id to false)
                            }
                        } catch (ignored: Exception) {
                            Timber.e(ignored)
                        }
                    }
                    contentListRepository.addItemsToList(addToList, localList)
                } catch (ignored: Exception) {
                    Timber.e(ignored)
                }
            }
        }
        return Result.success()
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return createForegroundInfo()
    }

    private fun createForegroundInfo(): ForegroundInfo {
        val id = TAG
        val title = "Refreshing user lists"
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
        const val TAG = "io.silv.UserListUpdateWorker"

        fun start(context: Context) {
            val workManager = WorkManager.getInstance(context)

            workManager.enqueueUniqueWork(
                TAG,
                ExistingWorkPolicy.KEEP,
                workRequest()
            )
        }

        fun workRequest() = OneTimeWorkRequestBuilder<UserListUpdateWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .addTag(TAG)
            .build()
    }
}