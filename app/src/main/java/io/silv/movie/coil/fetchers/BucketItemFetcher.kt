package io.silv.movie.coil.fetchers

import android.content.Context
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.SourceResult
import coil.key.Keyer
import coil.request.Options
import io.github.jan.supabase.storage.Storage
import io.silv.movie.coil.core.ByteArrayFetcherConfig
import io.silv.movie.coil.core.FetcherDiskStore
import io.silv.movie.coil.core.FetcherDiskStoreImageFile
import io.silv.movie.coil.fetchers.model.BucketFetchItem
import io.silv.movie.presentation.covers.cache.ProfileImageCache
import okio.Path.Companion.toOkioPath

class BucketItemFetcher(
    override val context: Context,
    private val storageLazy: Lazy<Storage>,
    private val profileImageCacheLazy: Lazy<ProfileImageCache>,
): ByteArrayFetcherConfig<BucketFetchItem> {

    private val storage
        get() = storageLazy.value

    private val profileImageCache
        get() = profileImageCacheLazy.value

    override val keyer: Keyer<BucketFetchItem> =
        Keyer { data, options ->
            "${data.bucket};${data.path}"
        }

    override val diskStore: FetcherDiskStore<BucketFetchItem>
        get() = FetcherDiskStoreImageFile { data, options ->
            if (data.bucket == "profile_pictures") {
                profileImageCache.getCoverFile(data.path)
            } else {
                null
            }
        }

    override suspend fun overrideFetch(options: Options, data: BucketFetchItem): FetchResult? {
        if (data.bucket == "profile_pictures") {
            val cover =   profileImageCache.getCoverFile(data.path)
                .takeIf { it?.exists() == true }
            return SourceResult(
                source = ImageSource(
                    file = cover?.toOkioPath() ?: return null,
                    diskCacheKey = keyer.key(data, options)
                ),
                mimeType = "image/*",
                dataSource = DataSource.DISK,
            )
        }
        return null
    }

    override suspend fun fetch(options: Options, data: BucketFetchItem): ByteArray {
        val bucket = storage[data.bucket]
        return bucket.downloadPublic(data.path)
    }
}