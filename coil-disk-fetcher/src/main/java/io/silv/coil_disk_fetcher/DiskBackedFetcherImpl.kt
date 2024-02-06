package io.silv.coil_disk_fetcher

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import androidx.core.graphics.drawable.toDrawable
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
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
import io.silv.coil_disk_fetcher.CoilDiskUtils.moveSnapshotToCoverCache
import io.silv.coil_disk_fetcher.CoilDiskUtils.readFromDiskCache
import io.silv.coil_disk_fetcher.CoilDiskUtils.toImageSource
import io.silv.coil_disk_fetcher.CoilDiskUtils.writeResponseToCoverCache
import io.silv.coil_disk_fetcher.CoilDiskUtils.writeToDiskCache
import io.silv.coil_disk_fetcher.CoilDiskUtils.writeToMemCache
import okio.Path.Companion.toOkioPath
import okio.buffer
import okio.source
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File


internal class DiskBackedFetcherImpl<T: Any>(
    val keyer: Keyer<T>,
    val diskStore: FetcherDiskStore<T>,
    val context: Context,
    val overrideCall: suspend (options: Options, data: T) -> FetchResult? = { _, _ -> null },
    val fetch: suspend (options: Options, data: T) -> ByteArray,
    val memoryCacheInit: () -> MemoryCache?,
    val diskCacheInit: () -> DiskCache?
): Fetcher.Factory<T>{

    internal val memCache by lazy { memoryCacheInit() ?: CoilMemoryCache.get(context) }
    internal val diskCache by lazy { diskCacheInit()  ?: CoilDiskCache.get(context) }

    private fun fileLoader(file: File, diskCacheKey: String): FetchResult {
        return SourceResult(
            source = ImageSource(
                file = file.toOkioPath(),
                diskCacheKey = diskCacheKey
            ),
            mimeType = "image/*",
            dataSource = DataSource.DISK,
        )
    }

    @OptIn(ExperimentalCoilApi::class)
    override fun create(data: T, options: Options, imageLoader: ImageLoader): Fetcher {

        val diskCacheKey = keyer.key(data, options) ?: error("null disk cache key provided")
        val memCacheKey = MemoryCache.Key(keyer.key(data, options) ?: error("null mem cache key provided"))

        return Fetcher {
            if (options.memoryCachePolicy.readEnabled) {
                memCache[memCacheKey]?.let {
                    return@Fetcher DrawableResult(
                        drawable = it.bitmap.toDrawable(context.resources),
                        isSampled = options.size.isOriginal,
                        dataSource = DataSource.MEMORY_CACHE
                    )
                }
            }

            overrideCall(options, data)?.let { fetchResult ->
                return@Fetcher fetchResult
                    .also {
                        writeToMemCache(
                            options,
                            bytes = when (fetchResult) {
                                is DrawableResult -> {
                                    val bitmap = (fetchResult.drawable as BitmapDrawable).bitmap
                                    val stream = ByteArrayOutputStream()
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                        bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSLESS, 100, stream)
                                    } else {
                                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                                    }
                                    stream.toByteArray()
                                }
                                is SourceResult -> fetchResult.source.source().buffer.readByteArray()
                            },
                            memCacheKey,
                            memCache
                        )
                    }
            }

            val imageCacheFile = diskStore.getImageFile(data, options)

            // Check if the file path already has an existing file meaning the image exists
            if (imageCacheFile?.exists() == true && options.diskCachePolicy.readEnabled) {
                return@Fetcher fileLoader(imageCacheFile, diskCacheKey)
                    .also {
                        writeToMemCache(options, imageCacheFile.readBytes(), memCacheKey, memCache)
                    }
            }

            var snapshot = readFromDiskCache(options, diskCache, diskCacheKey)

            try {
                // Fetch from disk cache
                if (snapshot != null) {

                    val snapshotCoverCache = moveSnapshotToCoverCache(
                        diskCache,
                        diskCacheKey,
                        snapshot,
                        imageCacheFile
                    )

                    if (snapshotCoverCache != null) {
                        // Read from cover cache after added to library
                        return@Fetcher fileLoader(snapshotCoverCache, diskCacheKey)
                            .also {
                                writeToMemCache(
                                    options,
                                    snapshotCoverCache.readBytes(),
                                    memCacheKey,
                                    memCache
                                )
                            }
                    }

                    // Read from snapshot
                    return@Fetcher SourceResult(
                        source = snapshot.toImageSource(diskCacheKey),
                        mimeType = "image/*",
                        dataSource = DataSource.DISK,
                    ).also {
                        writeToMemCache(
                            options,
                            snapshot!!.data.toFile().readBytes(),
                            memCacheKey,
                            memCache
                        )
                    }
                }
                // Fetch from network
                val response = fetch(options, data)
                try {
                    // Read from cover cache after library manga cover updated
                    val responseCoverCache = writeResponseToCoverCache(
                        ByteArrayInputStream(response),
                        imageCacheFile,
                        options
                    )
                    if (responseCoverCache != null) {
                        return@Fetcher fileLoader(responseCoverCache, diskCacheKey)
                    }

                    // Read from disk cache
                    snapshot = writeToDiskCache(response, diskCache, diskCacheKey)
                    if (snapshot != null) {
                        return@Fetcher SourceResult(
                            source = snapshot.toImageSource(diskCacheKey),
                            mimeType = "image/*",
                            dataSource = DataSource.NETWORK,
                        )
                    }

                    // Read from response if cache is unused or unusable
                    return@Fetcher SourceResult(
                        source = ImageSource(
                            source = ByteArrayInputStream(response).source().buffer(),
                            context = options.context
                        ),
                        mimeType = "image/*",
                        dataSource = DataSource.NETWORK,
                    )
                } catch (e: Exception) {
                    throw e
                } finally {
                    writeToMemCache(options, response, memCacheKey, memCache)
                }
            } catch (e: Exception) {
                snapshot?.close()
                throw e
            }
        }
    }
}