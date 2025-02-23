package io.silv.movie.presentation.screenmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.StateScreenModel
import io.github.jan.supabase.storage.Storage
import io.silv.core_ui.voyager.ioCoroutineScope
import io.silv.movie.coil.fetchers.model.BucketFetchItem
import io.silv.movie.presentation.covers.cache.ProfileImageCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

typealias ProfileImageContainer = Pair<String, List<BucketFetchItem>>

class SelectProfileImageScreenModel(
    private val storage: Storage,
    private val imageCache: ProfileImageCache,
): StateScreenModel<List<ProfileImageContainer>?>(null) {

    var refreshing by mutableStateOf(false)
        private set

    init {
        refreshImages()
    }

    fun refreshImages() {
        refreshing = true
        ioCoroutineScope.launch {


            runCatching {
                val profilePictures = storage["profile_pictures"]
                val folders = profilePictures.list()

                val items = folders.map { bucket ->
                    bucket.name to profilePictures.list(bucket.name)
                        .map {
                            BucketFetchItem(
                                "profile_pictures",
                                "${bucket.name}/${it.name}"
                            )
                        }
                }

                launch {
                    imageCache.clearOldProfilePicturesFromCache(
                        paths = items.map { (_, fetchItems) ->
                            fetchItems.map { it.path }
                        }
                            .flatten()
                    )
                }

                withContext(Dispatchers.Main) {
                    mutableState.update { items }
                }
            }
            withContext(Dispatchers.Main) { refreshing = false }
        }
    }
}
