package io.silv.movie.data.user

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
import io.silv.movie.data.lists.ContentListRepository
import io.silv.movie.data.lists.toUpdate
import io.silv.movie.data.movie.interactor.GetMovie
import io.silv.movie.data.movie.interactor.GetRemoteMovie
import io.silv.movie.data.movie.interactor.NetworkToLocalMovie
import io.silv.movie.data.movie.model.toDomain
import io.silv.movie.data.tv.interactor.GetRemoteTVShows
import io.silv.movie.data.tv.interactor.GetShow
import io.silv.movie.data.tv.interactor.NetworkToLocalTVShow
import io.silv.movie.data.tv.model.toDomain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import kotlinx.datetime.Clock
import timber.log.Timber

/**
 * Toggles favorite for movies and shows from the list in supabase.
 * Fetches the movie or show if it does not exist already.
 * No deletion / removals of favorites happen only additions for current signed in user.
 */
class UserListUpdateWorker (
    private val contentListRepository: ContentListRepository,
    private val listRepository: ListRepository,
    private val auth: Auth,
    private val getShow: GetShow,
    private val getMovie: GetMovie,
    private val getRemoteTVShows: GetRemoteTVShows,
    private val getRemoteMovie: GetRemoteMovie,
    private val networkToLocalMovie: NetworkToLocalMovie,
    private val networkToLocalTVShow: NetworkToLocalTVShow,
    private val userRepository: UserRepository,

    appContext: Context,
    private val params: WorkerParameters
): CoroutineWorker(appContext, params) {

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

            val network = userCreated.await().orEmpty() + subscriptions.await().orEmpty()

            for (list in network) {
                try {
                    var local = contentListRepository.getListForSupabaseId(list.listId)

                    if (local == null) {
                        val id = contentListRepository.createList(
                            name = list.name,
                            supabaseId = list.listId,
                            userId = list.userId,
                            inLibrary = true,
                            createdAt = list.createdAt.toEpochMilliseconds()
                        )
                        local = contentListRepository.getList(id)
                            ?: error("failed to add list $network")
                    }

                    Timber.d(local.toString())

                    contentListRepository.updateList(
                        local.copy(
                            description = list.description,
                            name = list.name,
                            username = username.await() ?: local.username,
                            public = list.public,
                            inLibrary = true,
                            lastSynced = Clock.System.now().toEpochMilliseconds()
                        )
                            .toUpdate()
                    )


                    val items = list.items.orEmpty()

                    val addToList = mutableListOf<Pair<Long, Boolean>>()

                    for (item in items) {
                        try {
                            if (item.movieId != -1L) {
                                var movie = getMovie.await(item.movieId)
                                if (movie == null) {
                                    movie = networkToLocalMovie.await(
                                        getRemoteMovie.awaitOne(item.movieId)!!.toDomain()
                                    )
                                }
                                addToList.add(movie.id to true)
                            } else if (item.showId != -1L) {
                                var show = getShow.await(item.showId)
                                if (show == null) {
                                    show = networkToLocalTVShow.await(
                                        getRemoteTVShows.awaitOne(item.showId)!!.toDomain()
                                    )
                                }
                                addToList.add(show.id to false)
                            }
                        } catch (ignored: Exception) {
                            Timber.e(ignored)
                        }
                    }
                    contentListRepository.addItemsToList(addToList, local)
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