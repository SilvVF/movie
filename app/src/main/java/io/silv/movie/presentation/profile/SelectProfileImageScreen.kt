package io.silv.movie.presentation.profile

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.koin.getScreenModel
import coil.compose.AsyncImage
import io.github.jan.supabase.storage.Storage
import io.silv.core_ui.voyager.ScreenResult
import io.silv.core_ui.voyager.ScreenWithResult
import io.silv.core_ui.voyager.ioCoroutineScope
import io.silv.movie.BucketFetchItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize

class SelectProfileImageScreenModel(
    private val storage: Storage,
): StateScreenModel<ImmutableList<BucketFetchItem>>(persistentListOf()) {

    init {
       refreshImages()
    }

    fun refreshImages() {
        ioCoroutineScope.launch {

            val items = storage["profile_pictures"].list()
            val bucketItems =
                items.map { BucketFetchItem("profile_pictures", it.name) }

            withContext(Dispatchers.Main) {
                mutableState.update {
                    bucketItems.toImmutableList()
                }
            }
        }
    }
}

object SelectProfileImageScreen: ScreenWithResult<SelectProfileImageScreen.ImageResult> {

    @Parcelize
    data class ImageResult(
        val path: String
    ): ScreenResult

    @Composable
    override fun Content() {

        val screenModel = getScreenModel<SelectProfileImageScreenModel>()
        val state by screenModel.state.collectAsStateWithLifecycle()

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize()
        ) {
            items(state) {
                AsyncImage(
                    model = it,
                    modifier = Modifier.fillMaxWidth(),
                    contentDescription = null
                )
            }
        }
    }
}