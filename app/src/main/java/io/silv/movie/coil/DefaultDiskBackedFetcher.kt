package io.silv.movie.coil

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import androidx.core.graphics.drawable.toDrawable
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.disk.DiskCache
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.key.Keyer
import coil.memory.MemoryCache
import coil.request.Options
import coil.size.isOriginal
import io.silv.movie.ContentPosterFetcher
import io.silv.movie.coil.CoilDiskUtils.toImageSource
import okio.Path.Companion.toOkioPath
import okio.buffer
import okio.source
import java.io.ByteArrayOutputStream
import java.io.File

abstract class DefaultDiskBackedFetcher<T: Any>(
    open val keyer: Keyer<T>,
    open val diskStore: FetcherDiskStore<T>,
    open val context: Context,
    open val memoryCacheInit: () -> MemoryCache,
    open val diskCacheInit: () -> DiskCache,
    open val overrideCall: suspend (options: Options, data: T) -> FetchResult?
): Fetcher.Factory<T> {
    internal val memCache
        get() = memoryCacheInit()

    internal val diskCache
        get() = diskCacheInit()

    override fun create(data: T, options: Options, imageLoader: ImageLoader): Fetcher? {
        return Fetcher {

            val diskCacheKey = keyer.key(data, options)
                ?: error("null disk cache key provided")

            val memCacheKey = MemoryCache.Key(keyer.key(data, options)
                ?: error("null mem cache key provided"))


            if (options.memoryCachePolicy.readEnabled) {
                memCache[memCacheKey]?.let { cachedValue ->
                    return@Fetcher DrawableResult(
                        drawable = cachedValue.bitmap.toDrawable(context.resources),
                        isSampled = options.size.isOriginal,
                        dataSource = DataSource.MEMORY_CACHE
                    )
                }
            }

            val overrideData = overrideCall(options, data)

            if (overrideData != null) {
                CoilDiskUtils.writeToMemCache(
                    options = options,
                    bytes =  overrideData.toBytes(),
                    memCacheKey = memCacheKey,
                    memCache = memCache
                )


                return@Fetcher overrideData
            }

            val imageCacheFile = diskStore.getImageFile(data, options)

            // Check if the file path already has an existing file meaning the image exists
            if (imageCacheFile?.exists() == true && options.diskCachePolicy.readEnabled) {
                CoilDiskUtils.writeToMemCache(
                    options,
                    imageCacheFile.source().buffer().readByteArray(),
                    memCacheKey,
                    memCache
                )
                return@Fetcher fileLoader(imageCacheFile, diskCacheKey, options)
            }

            val snapshot =
                CoilDiskUtils.readFromDiskCache(options, diskCache, diskCacheKey)

            try {
                if (snapshot != null) {
                    return@Fetcher fetchFromDiskCache(
                        snapshot,
                        diskCacheKey,
                        imageCacheFile,
                        options,
                        memCacheKey
                    )
                }
            } catch (e: Exception) {
                snapshot?.close()
            }

            fetch(data, options, memCacheKey, diskCacheKey, imageCacheFile)
        }
    }

    protected abstract suspend fun fetch(
        data: T,
        options: Options,
        memCacheKey: MemoryCache.Key,
        diskCacheKey: String,
        imageCacheFile: File?
    ): FetchResult

    private fun FetchResult.toBytes(): ByteArray? = when (this) {
        is DrawableResult -> {
            val bitmap = (this.drawable as? BitmapDrawable)?.bitmap
            bitmap?.let { bmp ->
                val stream = ByteArrayOutputStream()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    bmp.compress(
                        Bitmap.CompressFormat.WEBP_LOSSLESS,
                        100,
                        stream
                    )
                } else {
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, stream)
                }
                stream.toByteArray()
            }
        }
        is SourceResult -> this.source.source().buffer.readByteArray()
    }

    protected fun fileLoader(file: File, diskCacheKey: String, options: Options): FetchResult {
        return SourceResult(
            source = ImageSource(
                file = file.toOkioPath(),
                diskCacheKey = diskCacheKey.takeIf {
                    !(options.parameters.value(ContentPosterFetcher.DISABLE_KEYS) ?: false)
                }
            ),
            mimeType = "image/*",
            dataSource = DataSource.DISK,
        )
    }

    private fun fetchFromDiskCache(
        snapshot: DiskCache.Snapshot,
        diskCacheKey: String,
        imageCacheFile: File?,
        options: Options,
        memCacheKey: MemoryCache.Key
    ): FetchResult {
        // Fetch from disk cache
        val snapshotCoverCache = CoilDiskUtils.moveSnapshotToCoverCache(
            diskCache,
            diskCacheKey,
            snapshot,
            imageCacheFile
        )
        if (snapshotCoverCache != null) {
            CoilDiskUtils.writeToMemCache(
                options,
                snapshotCoverCache.source().buffer().readByteArray(),
                memCacheKey,
                memCache
            )
            // Read from cover cache after added to library
            return fileLoader(snapshotCoverCache, diskCacheKey, options)
        }
        CoilDiskUtils.writeToMemCache(
            options,
            snapshot.data.toFile().readBytes(),
            memCacheKey,
            memCache
        )
        // Read from snapshot
        return SourceResult(
            source = snapshot.toImageSource(diskCacheKey),
            mimeType = "image/*",
            dataSource = DataSource.DISK
        )
    }
}
